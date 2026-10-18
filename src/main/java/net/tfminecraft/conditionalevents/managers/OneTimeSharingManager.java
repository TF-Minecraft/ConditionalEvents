package net.tfminecraft.conditionalevents.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.character.OneTimeSharing;
import net.tfminecraft.conditionalevents.model.CEEvent;
import net.tfminecraft.conditionalevents.model.internal.AdditionalEventStorage;
import net.tfminecraft.conditionalevents.model.internal.CharacterRun;
import net.tfminecraft.conditionalevents.model.internal.ExecutedEvent;

/** The plugin's side of shared one-time events: which events share, and their {@code shared} action group. */
public class OneTimeSharingManager implements OneTimeSharing {
    public static final String SHARED_GROUP = "shared";

    private final ConditionalEvents plugin;

    public OneTimeSharingManager(ConditionalEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<String> sharedEvents() {
        List<String> names = new ArrayList<>();
        for (CEEvent event : plugin.getEventsManager().getEvents()) {
            if (event.isEnabled() && event.sharesOneTime()) {
                names.add(event.getName());
            }
        }
        return names;
    }

    @Override
    public void run(Player player, UUID character, String eventName, long resetEpoch) {
        CEEvent event = plugin.getEventsManager().getEvent(eventName);
        if (event == null || event.getActionGroup(SHARED_GROUP) == null) {
            return;
        }
        CharacterRun run = new CharacterRun(character);
        run.markedOneTime(resetEpoch);
        ExecutedEvent executed = new ExecutedEvent(player, new ArrayList<>(), event, SHARED_GROUP, null, null, plugin,
                new AdditionalEventStorage());
        executed.bindToCharacter(run);
        plugin.getDebugManager().sendActionsMessage(eventName, SHARED_GROUP, player);
        executed.executeActions();
    }

    @Override
    public void later(long ticks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
}
