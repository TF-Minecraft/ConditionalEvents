package net.tfminecraft.conditionalevents.model.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import net.tfminecraft.conditionalevents.model.player.PlayerData;

class ExecutedEventCharacterTest {
    private static final String EVENT = "codex_poi100";
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
        poi100 = new CEEvent(EVENT);
        poi100.setOneTime(true);
        when(tracker.character(owner)).thenReturn(Optional.of(aldric));
    }

    @Test
    void aRunCarriesOnWhileItsCharacterPlays() {
        ExecutedEvent run = run(new CharacterRun(aldric));

        assertTrue(run.stillForItsCharacter());
    }

    @Test
    void switchingDuringAWaitDropsTheRunAndItsGroupsAndUndoesItsMarksOnce() {
        CharacterRun record = admit(100L, 500L);
        ExecutedEvent parent = run(record);
        ExecutedEvent group = run(parent.getCharacterRun());
        when(tracker.character(owner)).thenReturn(Optional.of(mira));

        assertFalse(group.stillForItsCharacter());
        assertFalse(parent.stillForItsCharacter());

        assertFalse(players.getEventOneTime(EVENT, player, aldric));
        assertEquals(100, players.getEventCooldown(EVENT, player, aldric));
        assertFalse(players.getEventOneTime(EVENT, player, mira));
        assertSame(record, group.getCharacterRun());
    }

    @Test
    void anOldRunLeavesANewerCooldownAlone() {
        CharacterRun first = admit(0L, 1000L);
        players.setEventCooldown(EVENT, player, aldric, 2000);
        when(tracker.character(owner)).thenReturn(Optional.of(mira));

        assertFalse(run(first).stillForItsCharacter());

        assertEquals(2000, players.getEventCooldown(EVENT, player, aldric));
        assertFalse(players.getEventOneTime(EVENT, player, aldric));
    }

    @Test
    void anOldRunLeavesFlagsEarnedAfterAStaffResetAlone() {
        CharacterRun first = admit(null, null);
        players.getPlayer(player, false).resetEvent(EVENT);
        players.setEventOneTime(EVENT, player, aldric, true);
        when(tracker.character(owner)).thenReturn(Optional.of(mira));

        assertFalse(run(first).stillForItsCharacter());

        assertTrue(players.getEventOneTime(EVENT, player, aldric));
    }

    @Test
    void unloadedDataIsNotRecreatedByAnUndo() {
        CharacterRun record = admit(null, null);
        players.removePlayer(players.getPlayer(player, false));
        when(tracker.character(owner)).thenReturn(Optional.empty());

        assertFalse(run(record).stillForItsCharacter());

        assertNull(players.getPlayer(player, false));
    }

    @Test
    void aRunThatMarkedNothingUndoesNothing() {
        players.setEventOneTime(EVENT, player, aldric, true);
        when(tracker.character(owner)).thenReturn(Optional.of(mira));

        assertFalse(run(new CharacterRun(aldric)).stillForItsCharacter());

        assertTrue(players.getEventOneTime(EVENT, player, aldric));
    }

    @Test
    void accountRunsAreNeverDropped() {
        when(tracker.character(owner)).thenReturn(Optional.empty());

        assertTrue(run(null).stillForItsCharacter());
    }

    /** Marks as EventsManager does on admission: one-time always, the cooldown when given. */
    private CharacterRun admit(Long previousCooldown, Long setCooldown) {
        CharacterRun record = new CharacterRun(aldric);
        players.setEventOneTime(EVENT, player, aldric, true);
        PlayerData data = players.getPlayer(player, false);
        record.markedOneTime(data.getResetEpoch());
        if (setCooldown != null) {
            players.setEventCooldown(EVENT, player, aldric, setCooldown);
            record.markedCooldown(previousCooldown, setCooldown, data.getResetEpoch());
        }
        return record;
    }

    private ExecutedEvent run(CharacterRun record) {
        ExecutedEvent run = new ExecutedEvent(player, new ArrayList<>(), poi100, "default", null, null, plugin, null);
        run.bindToCharacter(record);
        return run;
    }
}
