package net.tfminecraft.conditionalevents.character;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * What sharing one-time events needs from the rest of the plugin. A per-character one-time event with
 * {@code share_one_time: true} is done for every character of an account once one of them has done it;
 * the others run its {@code shared} action group instead, for example to get its Player EXP.
 */
public interface OneTimeSharing {
    /** The names of the events whose one-time flag is shared. */
    Collection<String> sharedEvents();

    /**
     * Runs the event's {@code shared} action group for the player, if it has one, as a run of the
     * character it was shared with, so the flag is undone if that character leaves before it ends.
     */
    void run(Player player, UUID character, String event, long resetEpoch);

    /** Runs the task on the main thread after the given number of ticks. */
    void later(long ticks, Runnable task);
}
