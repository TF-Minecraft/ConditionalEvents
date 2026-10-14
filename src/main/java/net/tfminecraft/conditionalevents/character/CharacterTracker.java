package net.tfminecraft.conditionalevents.character;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
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
 * off the main thread. When a session starts, the account's data for per-character events moves
 * to the account's heir.
 */
public final class CharacterTracker implements Listener {
    private final Map<UUID, UUID> current = new ConcurrentHashMap<>();
    private final PlayerManager players;
    private final Predicate<String> characterScoped;
    private final Logger logger;

    public CharacterTracker(PlayerManager players, Predicate<String> characterScoped, Logger logger) {
        this.players = players;
        this.characterScoped = characterScoped;
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
        migrate(ref.ownerId());
        current.put(ref.ownerId(), ref.characterId());
    }

    /** Gives the account's data for per-character events to its heir. Main thread only. */
    void migrate(UUID owner) {
        PlayerData data = players.getPlayerByUUID(owner);
        if (data == null) {
            return;
        }
        Heirs.of(owner).ifPresent(heir -> {
            int moved = data.moveToCharacter(heir, characterScoped);
            if (moved > 0) {
                logger.info("Moved " + moved + " per-character event records of account " + owner + " to character " + heir);
            }
        });
    }
}
