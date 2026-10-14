package net.tfminecraft.conditionalevents.character;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.conditionalevents.managers.PlayerManager;
import net.tfminecraft.conditionalevents.model.player.PlayerData;
import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.Characters;

/**
 * Which character each player is playing, readable from any thread, since events such as chat run
 * off the main thread. On an account's first session it also gives the account's old data for
 * per-character events to its most-played character, matching what Codex does with old journals.
 */
public final class CharacterTracker implements Listener {
    private final Map<UUID, UUID> current = new ConcurrentHashMap<>();
    private final PlayerManager players;
    private final Predicate<String> perCharacter;
    private final LegacyRecipient recipients;
    private final Logger logger;

    public CharacterTracker(PlayerManager players, Predicate<String> perCharacter, LegacyRecipient recipients,
            Logger logger) {
        this.players = players;
        this.perCharacter = perCharacter;
        this.recipients = recipients;
        this.logger = logger;
    }

    /** The character the player is playing, or empty between characters. */
    public Optional<UUID> character(UUID account) {
        return Optional.ofNullable(current.get(account));
    }

    /** Picks up characters already in play when the plugin enables. Main thread only. */
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Characters.currentSession(player).ifPresent(session -> begin(session.ref()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStart(CharacterSessionStartEvent event) {
        begin(event.getSession().ref());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(CharacterSessionEndEvent event) {
        CharacterRef ref = event.getSession().ref();
        current.remove(ref.ownerId(), ref.characterId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        current.remove(event.getPlayer().getUniqueId());
    }

    private void begin(CharacterRef ref) {
        seed(ref.ownerId());
        current.put(ref.ownerId(), ref.characterId());
    }

    /**
     * Once per account, main thread only, to the character pinned for the account (see
     * {@link LegacyRecipient}). Waits for a later session if no character can be chosen yet.
     */
    void seed(UUID owner) {
        PlayerData data = players.getPlayerByUUID(owner);
        if (data == null || data.getSeededTo() != null) {
            return;
        }
        Optional<UUID> heir;
        try {
            heir = recipients.of(owner, () -> MostPlayed.of(Characters.characters(owner)));
        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Could not pin the most-played character of account " + owner
                    + "; trying again at its next session", ex);
            return;
        }
        heir.ifPresent(character -> {
            int moved = data.seed(character, perCharacter);
            if (moved > 0) {
                logger.info("Gave " + moved + " old per-character event records of account " + owner
                        + " to its most-played character " + character);
            }
        });
    }
}
