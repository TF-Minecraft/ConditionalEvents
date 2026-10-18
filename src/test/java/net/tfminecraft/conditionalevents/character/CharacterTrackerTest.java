package net.tfminecraft.conditionalevents.character;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
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
    @TempDir
    Path plugins;

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID alt = UUID.randomUUID();
    private MockedStatic<Characters> characters;
    private final List<LogRecord> logs = new ArrayList<>();
    private PlayerManager players;
    private CharacterTracker tracker;
    private Player player;
    private FakeSharing sharing;

    private record Run(UUID character, String event, long epoch) {
    }

    private static final class FakeSharing implements OneTimeSharing {
        List<String> events = List.of();
        final List<Long> delays = new ArrayList<>();
        final List<Runnable> tasks = new ArrayList<>();
        final List<Run> runs = new ArrayList<>();

        @Override
        public java.util.Collection<String> sharedEvents() {
            return events;
        }

        @Override
        public void run(Player player, UUID character, String event, long resetEpoch) {
            runs.add(new Run(character, event, resetEpoch));
        }

        @Override
        public void later(long ticks, Runnable task) {
            delays.add(ticks);
            tasks.add(task);
        }

        void runAll() {
            List<Runnable> due = new ArrayList<>(tasks);
            tasks.clear();
            due.forEach(Runnable::run);
        }
    }

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
        sharing = new FakeSharing();
        tracker = new CharacterTracker(players, Set.of("research_bronze")::contains, new LegacyRecipient(plugins), sharing,
                logger);
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
    void thePinnedCharacterWinsOverTodaysMostPlayed() throws Exception {
        UUID pinned = UUID.randomUUID();
        Files.createDirectories(plugins.resolve("TLibs/legacy-recipients"));
        Files.writeString(plugins.resolve("TLibs/legacy-recipients/" + owner + ".txt"), pinned.toString());
        PlayerData data = new PlayerData(owner, "Steve");
        data.setOneTime("research_bronze", true);
        players.addPlayer(data);
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(played(alt, 9000))));

        tracker.onStart(start(alt));

        assertTrue(data.isEventOneTime(pinned, "research_bronze"));
        assertFalse(data.isEventOneTime(alt, "research_bronze"));
    }

    @Test
    void aPinThatCannotBeWrittenWaitsForTheNextSession() throws Exception {
        Files.writeString(plugins.resolve("TLibs"), "not a folder");
        PlayerData data = new PlayerData(owner, "Steve");
        players.addPlayer(data);
        characters.when(() -> Characters.characters(owner)).thenReturn(Optional.of(List.of(played(alt, 5))));

        tracker.onStart(start(alt));

        assertNull(data.getSeededTo());
        assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("Could not pin the most-played character")));
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

    @Test
    void aCharacterGetsTheSharedEventsItsAccountHasDone() {
        sharing.events = List.of("duchy_1", "duchy_2", "duchy_3");
        when(player.isOnline()).thenReturn(true);
        PlayerData data = new PlayerData(owner, "Steve");
        data.setSeededTo(other);
        data.setOneTime(other, "duchy_1", true);
        data.setOneTime(other, "duchy_2", true);
        data.setOneTime(alt, "duchy_2", true);
        data.setOneTime("duchy_3", true);
        data.resetEvent("duchy_1");
        data.setOneTime(other, "duchy_1", true);
        players.addPlayer(data);

        tracker.onStart(start(alt));
        assertEquals(List.of(CharacterTracker.SHARE_DELAY_TICKS), sharing.delays);
        assertFalse(data.isEventOneTime(alt, "duchy_1"), "Nothing arrives before the delay");
        sharing.runAll();

        assertTrue(data.isEventOneTime(alt, "duchy_1"));
        assertFalse(data.isEventOneTime(alt, "duchy_3"), "Account-level data is not a character's");
        assertEquals(List.of(new Run(alt, "duchy_1", 1)), sharing.runs, "Only what the character lacked runs");
        assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains(
                "Shared 1 one-time events of account " + owner + " with its character " + alt)));

        tracker.onStart(start(alt));
        sharing.runAll();
        assertEquals(1, sharing.runs.size(), "Nothing is shared twice");
    }

    @Test
    void sharingWaitsForTheSameCharacterToStillBeInPlay() {
        sharing.events = List.of("duchy_1");
        PlayerData data = new PlayerData(owner, "Steve");
        data.setSeededTo(other);
        data.setOneTime(other, "duchy_1", true);

        tracker.onStart(start(alt));
        sharing.runAll();
        players.addPlayer(data);
        tracker.onStart(start(alt));
        sharing.runAll();
        when(player.isOnline()).thenReturn(true);
        tracker.onStart(start(alt));
        tracker.onEnd(end(alt));
        sharing.runAll();
        tracker.onStart(start(alt));
        tracker.onStart(start(other));
        sharing.runAll();

        assertFalse(data.isEventOneTime(alt, "duchy_1"));
        assertTrue(sharing.runs.isEmpty(), "No player data, an offline player or a character no longer in play get nothing");
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
