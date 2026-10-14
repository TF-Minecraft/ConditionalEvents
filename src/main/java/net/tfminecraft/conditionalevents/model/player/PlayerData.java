package net.tfminecraft.conditionalevents.model.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * An account's event data. Events with {@code data_scope: character} keep theirs per character;
 * methods taking a {@code character} use the account's own list when it is null.
 */
public class PlayerData {
    private UUID uuid;
    private String name;
    private boolean modified;
    private ArrayList<EventData> eventData;
    private final Map<UUID, ArrayList<EventData>> characterEventData = new ConcurrentHashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.eventData = new ArrayList<>();
        this.modified = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<EventData> getEventData() {
        return eventData;
    }

    public void setEventData(ArrayList<EventData> eventData) {
        this.eventData = eventData;
    }

    public Map<UUID, ArrayList<EventData>> getCharacterEventData() {
        return characterEventData;
    }

    /** The account's or a character's list, created for a character on first write. */
    private ArrayList<EventData> writable(UUID character) {
        return character == null ? eventData : characterEventData.computeIfAbsent(character, ignored -> new ArrayList<>());
    }

    public EventData getEventData(String eventName){
        return getEventData(null, eventName);
    }

    public EventData getEventData(UUID character, String eventName){
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

    /** Resets the event for the account and every character. */
    public void resetEvent(String eventName){
        eventData.removeIf(c -> c.getName().equals(eventName));
        characterEventData.values().forEach(list -> list.removeIf(c -> c.getName().equals(eventName)));
        modified = true;
    }

    public void resetAll(){
        eventData.clear();
        characterEventData.clear();
        modified = true;
    }

    public void setCooldown(String eventName,long currentMillis){
        setCooldown(null, eventName, currentMillis);
    }

    public void setCooldown(UUID character, String eventName, long currentMillis){
        EventData e = getEventData(character, eventName);
        if(e == null){
            e = new EventData(eventName,currentMillis,false);
            writable(character).add(e);
        }else{
            e.setCooldown(currentMillis);
        }
    }

    public long getCooldown(String eventName){
        return getCooldown(null, eventName);
    }

    public long getCooldown(UUID character, String eventName){
        EventData e = getEventData(character, eventName);
        if(e == null){
            return 0;
        }else{
            return e.getCooldown();
        }
    }

    public void setOneTime(String eventName,boolean oneTime){
        setOneTime(null, eventName, oneTime);
    }

    public void setOneTime(UUID character, String eventName, boolean oneTime){
        EventData e = getEventData(character, eventName);
        if(e == null){
            e = new EventData(eventName,0,oneTime);
            writable(character).add(e);
        }else{
            e.setOneTime(oneTime);
        }
    }

    public boolean isEventOneTime(String eventName){
        return isEventOneTime(null, eventName);
    }

    public boolean isEventOneTime(UUID character, String eventName){
        EventData e = getEventData(character, eventName);
        if(e == null){
            return false;
        }else{
            return e.isOneTime();
        }
    }

    /**
     * Moves the account's data for matching events to a character, merging with what the character
     * already has: an event done once stays done, and the later cooldown start wins.
     * @return how many events moved
     */
    public int moveToCharacter(UUID character, Predicate<String> events){
        int moved = 0;
        Iterator<EventData> account = eventData.iterator();
        while(account.hasNext()){
            EventData e = account.next();
            if(!events.test(e.getName())){
                continue;
            }
            EventData own = getEventData(character, e.getName());
            if(own == null){
                writable(character).add(new EventData(e.getName(), e.getCooldown(), e.isOneTime()));
            }else{
                own.setOneTime(own.isOneTime() || e.isOneTime());
                own.setCooldown(Math.max(own.getCooldown(), e.getCooldown()));
            }
            account.remove();
            moved++;
        }
        if(moved > 0){
            modified = true;
        }
        return moved;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
