package net.tfminecraft.conditionalevents.character;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.tlibs.character.CharacterRef;
import net.tfminecraft.tlibs.character.CharacterSessionEndEvent;
import net.tfminecraft.tlibs.character.CharacterSessionStartEvent;
import net.tfminecraft.tlibs.character.Characters;

/**
 * Which character each player is playing, readable from any thread, since events such as chat run
 * off the main thread.
 */
public final class CharacterTracker implements Listener {
    private final Map<UUID, UUID> current = new ConcurrentHashMap<>();

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
        current.put(ref.ownerId(), ref.characterId());
    }

}
