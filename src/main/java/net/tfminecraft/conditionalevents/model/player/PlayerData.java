package net.tfminecraft.conditionalevents.model.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * An account's event data. Events with {@code data_scope: character} keep theirs per character;
 * methods taking a {@code character} use the account's own list when it is null.
 *
 * <p>Events run on the main thread and on async threads while saves run async, so every access
 * locks this object and saves work from a {@link #snapshot()}.
 */
public class PlayerData {
    private UUID uuid;
    private String name;
    private boolean modified;
    private long revision;
    private UUID seededTo;
    private long resetAllCount;
    private final Map<String, Long> resetCounts = new HashMap<>();
    private ArrayList<EventData> eventData;
    private final Map<UUID, ArrayList<EventData>> characterEventData = new HashMap<>();
    private final Object saveLock = new Object();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.eventData = new ArrayList<>();
        this.modified = false;
    }

    /** A copy of the data to write, with the revision it was taken at. */
    public record Snapshot(String name, List<EventData> account, Map<UUID, List<EventData>> characters, UUID seededTo,
            long revision) {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    /** Held while writing this account's file, so two saves never write it at once. */
    public Object saveLock() {
        return saveLock;
    }

    public synchronized void setEventData(ArrayList<EventData> eventData) {
        this.eventData = eventData;
    }

    /** Loading only. */
    public synchronized void setCharacterEventData(UUID character, ArrayList<EventData> events) {
        characterEventData.put(character, events);
    }

    /** Loading only: the character this account's old data was given to, or null if not yet. */
    public synchronized void setSeededTo(UUID seededTo) {
        this.seededTo = seededTo;
    }

    public synchronized UUID getSeededTo() {
        return seededTo;
    }

    /**
     * Once per account: moves its account-level data for the matching events to the character,
     * merging with what the character has (done stays done, the later cooldown start wins), and
     * records the character so it never happens again.
     * @return how many events moved, or -1 if the account was already seeded
     */
    public synchronized int seed(UUID character, Predicate<String> events) {
        if (seededTo != null) {
            return -1;
        }
        int moved = 0;
        Iterator<EventData> account = eventData.iterator();
        while (account.hasNext()) {
            EventData e = account.next();
            if (!events.test(e.getName())) {
                continue;
            }
            EventData own = find(character, e.getName());
            if (own == null) {
                writable(character).add(new EventData(e.getName(), e.getCooldown(), e.isOneTime()));
            } else {
                own.setOneTime(own.isOneTime() || e.isOneTime());
                own.setCooldown(Math.max(own.getCooldown(), e.getCooldown()));
            }
            account.remove();
            moved++;
        }
        seededTo = character;
        touched();
        return moved;
    }

    public synchronized Snapshot snapshot() {
        Map<UUID, List<EventData>> characters = new HashMap<>();
        characterEventData.forEach((character, events) -> characters.put(character, copy(events)));
        return new Snapshot(name, copy(eventData), characters, seededTo, revision);
    }

    private static List<EventData> copy(List<EventData> events) {
        List<EventData> copy = new ArrayList<>();
        for (EventData e : events) {
            copy.add(new EventData(e.getName(), e.getCooldown(), e.isOneTime()));
        }
        return copy;
    }

    /** Marks the data clean only if nothing changed since the snapshot was taken. */
    public synchronized void markSaved(long savedRevision) {
        if (savedRevision == revision) {
            modified = false;
        }
    }

    private ArrayList<EventData> writable(UUID character) {
        return character == null ? eventData : characterEventData.computeIfAbsent(character, ignored -> new ArrayList<>());
    }

    private EventData find(UUID character, String eventName) {
        List<EventData> events = character == null ? eventData : characterEventData.get(character);
        if(events == null){
            return null;
        }
        for(EventData e : events){
            if(e.getName().equals(eventName)){
                return e;
            }
        }
        return null;
    }

    private void touched() {
        revision++;
        modified = true;
    }

    /**
     * Changes whenever staff reset this event or all events, so an older run never undoes marks
     * made after such a reset. Resets of other events leave it alone.
     */
    public synchronized long getResetEpoch(String eventName){
        return resetAllCount + resetCounts.getOrDefault(eventName, 0L);
    }

    /**
     * Undoes what one abandoned run set, only where it still holds: the one-time flag unless the
     * event was reset since, and the cooldown only if it is still the start this run wrote.
     */
    public synchronized void undoRun(UUID character, String eventName, boolean oneTime, Long previousCooldown,
            long setCooldown, long epoch){
        EventData e = find(character, eventName);
        if(e == null || epoch != getResetEpoch(eventName)){
            return;
        }
        if(oneTime){
            e.setOneTime(false);
        }
        if(previousCooldown != null && e.getCooldown() == setCooldown){
            e.setCooldown(previousCooldown);
        }
        touched();
    }

    /** Resets the event for the account and every character. */
    public synchronized void resetEvent(String eventName){
        resetCounts.merge(eventName, 1L, Long::sum);
        eventData.removeIf(c -> c.getName().equals(eventName));
        characterEventData.values().forEach(list -> list.removeIf(c -> c.getName().equals(eventName)));
        touched();
    }

    public synchronized void resetAll(){
        resetAllCount++;
        eventData.clear();
        characterEventData.clear();
        touched();
    }

    public void setCooldown(String eventName,long currentMillis){
        setCooldown(null, eventName, currentMillis);
    }

    public synchronized void setCooldown(UUID character, String eventName, long currentMillis){
        EventData e = find(character, eventName);
        if(e == null){
            writable(character).add(new EventData(eventName,currentMillis,false));
        }else{
            e.setCooldown(currentMillis);
        }
        touched();
    }

    public long getCooldown(String eventName){
        return getCooldown(null, eventName);
    }

    public synchronized long getCooldown(UUID character, String eventName){
        EventData e = find(character, eventName);
        return e == null ? 0 : e.getCooldown();
    }

    public void setOneTime(String eventName,boolean oneTime){
        setOneTime(null, eventName, oneTime);
    }

    public synchronized void setOneTime(UUID character, String eventName, boolean oneTime){
        EventData e = find(character, eventName);
        if(e == null){
            writable(character).add(new EventData(eventName,0,oneTime));
        }else{
            e.setOneTime(oneTime);
        }
        touched();
    }

    public boolean isEventOneTime(String eventName){
        return isEventOneTime(null, eventName);
    }

    public synchronized boolean isEventOneTime(UUID character, String eventName){
        EventData e = find(character, eventName);
        return e != null && e.isOneTime();
    }

    public synchronized boolean isModified() {
        return modified;
    }

    public synchronized void setModified(boolean modified) {
        if(modified){
            touched();
        }else{
            this.modified = false;
        }
    }
}
