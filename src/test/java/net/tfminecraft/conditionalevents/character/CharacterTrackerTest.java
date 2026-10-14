package net.tfminecraft.conditionalevents.character;

import net.tfminecraft.tlibs.character.CharacterStatus;
import net.tfminecraft.tlibs.character.CharacterInfo;
import net.tfminecraft.conditionalevents.model.player.PlayerData;
import net.tfminecraft.conditionalevents.managers.PlayerManager;
import net.tfminecraft.conditionalevents.ConditionalEvents;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.Set;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSession;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.Characters;

class CharacterTrackerTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID alt = UUID.randomUUID();
    private MockedStatic<Characters> characters;
    private final List<LogRecord> logs = new ArrayList<>();
    private PlayerManager players;
    private CharacterTracker tracker;
    private Player player;

    @BeforeEach
    void setUp() {
        characters = mockStatic(Characters.class);
        players = new PlayerManager(mock(ConditionalEvents.class));
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        tracker = new CharacterTracker(players, Set.of("research_bronze")::contains, logger);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(owner);
    }

    @AfterEach
    void tearDown() {
        characters.close();
    }

    @Test
    void tracksThePlayedCharacterAcrossSwitches() {
        assertTrue(tracker.character(owner).isEmpty());

        tracker.onStart(start(alt));
        assertEquals(Optional.of(alt), tracker.character(owner));

        tracker.onEnd(end(other));
        assertEquals(Optional.of(alt), tracker.character(owner));

        tracker.onEnd(end(alt));
        assertTrue(tracker.character(owner).isEmpty());
    }

    @Test
    void quittingForgetsTheCharacter() {
        tracker.onStart(start(alt));

        tracker.onQuit(new PlayerQuitEvent(player, "bye"));

        assertTrue(tracker.character(owner).isEmpty());
    }

    @Test
    void theMostPlayedCharacterGetsTheOldPerCharacterDataOnce() {
        PlayerData data = new PlayerData(owner, "Steve");
        data.setOneTime("research_bronze", true);
        data.setCooldown("global_booster", 900);
        players.addPlayer(data);
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(
                played(other, 9000), played(alt, 100))));

        tracker.onStart(start(alt));

        assertTrue(data.isEventOneTime(other, "research_bronze"));
        assertFalse(data.isEventOneTime(alt, "research_bronze"));
        assertFalse(data.isEventOneTime("research_bronze"));
        assertEquals(900, data.getCooldown("global_booster"));
        assertEquals(other, data.getSeededTo());
        assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("Gave 1 old per-character event records")));

        data.setOneTime("research_bronze", true);
        tracker.onStart(start(alt));
        assertTrue(data.isEventOneTime("research_bronze"));
    }

    @Test
    void noSeedWithoutDataOrRosterAndNothingToMoveIsQuiet() {
        tracker.onStart(start(alt));

        PlayerData data = new PlayerData(owner, "Steve");
        players.addPlayer(data);
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.empty());
        tracker.onStart(start(alt));
        assertNull(data.getSeededTo());

        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(played(alt, 5))));
        tracker.onStart(start(alt));
        assertEquals(alt, data.getSeededTo());
        assertTrue(logs.isEmpty());
    }

    private CharacterInfo played(UUID character, long seconds) {
        return new CharacterInfo(new CharacterRef(owner, character), "Name", CharacterStatus.ALIVE, null, Set.of(), seconds, null);
    }

    @Test
    void startPicksUpCharactersAlreadyInPlay() {
        characters.when(() -> Characters.currentSession(player))
                .thenReturn(Optional.of(new CharacterSession(new CharacterRef(owner, alt), 1)));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));

            tracker.start();
        }

        assertEquals(Optional.of(alt), tracker.character(owner));
    }

    private CharacterSessionStartEvent start(UUID character) {
        return new CharacterSessionStartEvent(player, new CharacterSession(new CharacterRef(owner, character), 1));
    }

    private CharacterSessionEndEvent end(UUID character) {
        return new CharacterSessionEndEvent(player, new CharacterSession(new CharacterRef(owner, character), 1),
                CharacterSessionEndEvent.Reason.SWITCH);
    }
}
