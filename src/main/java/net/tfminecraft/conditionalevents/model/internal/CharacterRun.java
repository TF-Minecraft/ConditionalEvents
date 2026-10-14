package net.tfminecraft.conditionalevents.model.internal;

import java.util.UUID;

import net.tfminecraft.conditionalevents.model.player.PlayerData;

/**
 * One admitted run of a per-character event, shared by the run and every action group it calls.
 * It records the character the run belongs to and the marks it set, so a run whose character has
 * left can be dropped once and undo exactly those marks.
 */
public final class CharacterRun {
    private final UUID character;
    private boolean markedOneTime;
    private Long previousCooldown;
    private long setCooldown;
    private long resetEpoch;
    private boolean abandoned;

    public CharacterRun(UUID character) {
        this.character = character;
    }

    public UUID character() {
        return character;
    }

    /** Records the one-time flag this run set, under the data's current reset epoch. */
    public void markedOneTime(long resetEpoch) {
        this.markedOneTime = true;
        this.resetEpoch = resetEpoch;
    }

    /** Records the cooldown start this run set and the value it replaced. */
    public void markedCooldown(long previous, long set, long resetEpoch) {
        this.previousCooldown = previous;
        this.setCooldown = set;
        this.resetEpoch = resetEpoch;
    }

    public boolean isAbandoned() {
        return abandoned;
    }

    /**
     * Drops the run and undoes its marks in the loaded data, if any. Returns false if it was already
     * dropped. Data no longer loaded is left alone rather than recreated.
     */
    public synchronized boolean abandon(PlayerData data, String eventName) {
        if (abandoned) {
            return false;
        }
        abandoned = true;
        if (data != null && (markedOneTime || previousCooldown != null)) {
            data.undoRun(character, eventName, markedOneTime, previousCooldown, setCooldown, resetEpoch);
        }
        return true;
    }
}
