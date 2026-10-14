package net.tfminecraft.conditionalevents.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.character.CharacterTracker;
import net.tfminecraft.conditionalevents.model.CEEvent;
import net.tfminecraft.conditionalevents.model.player.PlayerData;

class PlayerManagerScopeTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID character = UUID.randomUUID();
    private CharacterTracker tracker;
    private PlayerManager players;
    private Player player;

    @BeforeEach
    void setUp() {
        ConditionalEvents plugin = mock(ConditionalEvents.class);
        EventsManager events = mock(EventsManager.class);
        tracker = mock(CharacterTracker.class);
        when(plugin.getEventsManager()).thenReturn(events);
        when(plugin.getCharacterTracker()).thenReturn(tracker);
        CEEvent research = new CEEvent("research_bronze");
        research.setOneTime(true);
        CEEvent booster = new CEEvent("global_booster");
        booster.setOneTime(true);
        booster.setCharacterScoped(false);
        when(events.getEvent("research_bronze")).thenReturn(research);
        when(events.getEvent("global_booster")).thenReturn(booster);
        players = new PlayerManager(plugin);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(owner);
        when(player.getName()).thenReturn("Steve");
        when(tracker.character(owner)).thenReturn(Optional.empty());
    }

    @Test
    void perCharacterEventsHaveNoDataBetweenCharacters() {
        assertNull(players.playedCharacter(player));

        players.setEventOneTime("research_bronze", player);
        players.setEventCooldown("research_bronze", player);

        assertFalse(players.getEventOneTime("research_bronze", player));
        assertEquals(0, players.getEventCooldown("research_bronze", player));
        assertNull(players.getPlayer(player, false));
    }

    @Test
    void perCharacterEventsUseThePlayedCharactersData() {
        when(tracker.character(owner)).thenReturn(Optional.of(character));

        players.setEventOneTime("research_bronze", player);
        players.setEventCooldown("research_bronze", player);
        players.setEventOneTime("global_booster", player);

        PlayerData data = players.getPlayer(player, false);
        assertEquals(character, players.playedCharacter(player));
        assertTrue(players.getEventOneTime("research_bronze", player));
        assertTrue(players.getEventCooldown("research_bronze", player) > 0);
        assertTrue(data.isEventOneTime(character, "research_bronze"));
        assertFalse(data.isEventOneTime("research_bronze"));
        assertTrue(data.isEventOneTime("global_booster"));
        assertTrue(players.getEventOneTime("global_booster", player));
        assertEquals(0, players.getEventCooldown("unknown_event", player));
    }

    @Test
    void explicitCharacterCallsIgnoreWhoIsPlayingNow() {
        UUID other = UUID.randomUUID();
        when(tracker.character(owner)).thenReturn(Optional.of(other));

        players.setEventOneTime("research_bronze", player, character, true);
        players.setEventCooldown("research_bronze", player, character, 42);

        assertTrue(players.getEventOneTime("research_bronze", player, character));
        assertEquals(42, players.getEventCooldown("research_bronze", player, character));
        assertFalse(players.getEventOneTime("research_bronze", player));
    }
}
