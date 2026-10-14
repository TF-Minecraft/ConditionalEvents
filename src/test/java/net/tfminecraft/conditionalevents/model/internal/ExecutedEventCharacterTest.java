package net.tfminecraft.conditionalevents.model.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.character.CharacterTracker;
import net.tfminecraft.conditionalevents.managers.DebugManager;
import net.tfminecraft.conditionalevents.managers.EventsManager;
import net.tfminecraft.conditionalevents.managers.PlayerManager;
import net.tfminecraft.conditionalevents.model.CEEvent;

class ExecutedEventCharacterTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID aldric = UUID.randomUUID();
    private final UUID mira = UUID.randomUUID();
    private ConditionalEvents plugin;
    private CharacterTracker tracker;
    private PlayerManager players;
    private Player player;
    private CEEvent poi100;

    @BeforeEach
    void setUp() {
        plugin = mock(ConditionalEvents.class);
        tracker = mock(CharacterTracker.class);
        when(plugin.getCharacterTracker()).thenReturn(tracker);
        when(plugin.getEventsManager()).thenReturn(mock(EventsManager.class));
        when(plugin.getDebugManager()).thenReturn(mock(DebugManager.class));
        players = new PlayerManager(plugin);
        when(plugin.getPlayerManager()).thenReturn(players);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(owner);
        when(player.getName()).thenReturn("Steve");
        poi100 = new CEEvent("codex_poi100");
        poi100.setCharacterScoped(true);
    }

    @Test
    void aRunCarriesOnWhileItsCharacterPlays() {
        when(tracker.character(owner)).thenReturn(Optional.of(aldric));
        ExecutedEvent run = run();
        run.bindToCharacter(aldric, true, null);

        assertTrue(run.stillForItsCharacter());
    }

    @Test
    void switchingDuringAWaitDropsTheRunAndUndoesItsMarks() {
        players.setEventOneTime("codex_poi100", player, aldric, true);
        players.setEventCooldown("codex_poi100", player, aldric, 500);
        ExecutedEvent run = run();
        run.bindToCharacter(aldric, true, 100L);
        when(tracker.character(owner)).thenReturn(Optional.of(mira));

        assertFalse(run.stillForItsCharacter());
        assertFalse(run.stillForItsCharacter());

        assertFalse(players.getEventOneTime("codex_poi100", player, aldric));
        assertEquals(100, players.getEventCooldown("codex_poi100", player, aldric));
        assertFalse(players.getEventOneTime("codex_poi100", player, mira));
    }

    @Test
    void anAbandonedRunWithNothingToUndoLeavesDataAlone() {
        players.setEventOneTime("codex_poi100", player, aldric, true);
        ExecutedEvent run = run();
        run.bindToCharacter(aldric, false, null);
        when(tracker.character(owner)).thenReturn(Optional.empty());

        assertFalse(run.stillForItsCharacter());
        assertTrue(players.getEventOneTime("codex_poi100", player, aldric));
    }

    @Test
    void accountRunsAreNeverDropped() {
        ExecutedEvent run = run();
        run.bindToCharacter(null, true, null);

        assertTrue(run.stillForItsCharacter());
    }

    private ExecutedEvent run() {
        return new ExecutedEvent(player, new ArrayList<>(), poi100, "default", null, null, plugin, null);
    }
}
