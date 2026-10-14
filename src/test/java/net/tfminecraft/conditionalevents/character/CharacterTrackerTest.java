package net.tfminecraft.conditionalevents.character;

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
    private CharacterTracker tracker;
    private Player player;

    @BeforeEach
    void setUp() {
        characters = mockStatic(Characters.class);
        tracker = new CharacterTracker();
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
