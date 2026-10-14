package net.tfminecraft.conditionalevents.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import net.tfminecraft.conditionalevents.model.player.PlayerData;

class PlayersConfigsManagerTest {
    private final UUID account = UUID.randomUUID();
    private final UUID character = UUID.randomUUID();

    @Test
    void characterEventsRoundTripBesideAccountEvents() throws Exception {
        PlayerData data = new PlayerData(account, "Steve");
        data.setCooldown("global_booster", 900);
        data.setOneTime(character, "research_bronze", true);
        YamlConfiguration yaml = new YamlConfiguration();

        PlayersConfigsManager.write(data.snapshot(), yaml);
        YamlConfiguration reread = new YamlConfiguration();
        reread.loadFromString(yaml.saveToString());
        PlayerData loaded = PlayersConfigsManager.read(account, reread);

        assertEquals("Steve", loaded.getName());
        assertEquals(900, loaded.getCooldown("global_booster"));
        assertTrue(loaded.isEventOneTime(character, "research_bronze"));
        assertFalse(loaded.isEventOneTime("research_bronze"));
    }

    @Test
    void upstreamFilesStillLoadAndStrayCharacterKeysAreIgnored() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                name: Steve
                events:
                  codex_die:
                    one_time: true
                    cooldown: 0
                characters:
                  notes:
                    events:
                      x:
                        one_time: true
                  %s:
                    other: 1
                """.formatted(character));

        PlayerData loaded = PlayersConfigsManager.read(account, yaml);

        assertTrue(loaded.isEventOneTime("codex_die"));
        assertEquals(1, loaded.snapshot().characters().size());
        assertTrue(loaded.snapshot().characters().get(character).isEmpty());
    }

    @Test
    void resetClearsTheEventForTheAccountAndEveryCharacter() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                name: Steve
                events:
                  codex_die: {one_time: true}
                  codex_tf: {one_time: true}
                characters:
                  %s:
                    events:
                      codex_die: {one_time: true}
                """.formatted(character));

        assertTrue(PlayersConfigsManager.reset(yaml, "codex_die"));
        assertFalse(yaml.contains("events.codex_die"));
        assertFalse(yaml.contains("characters." + character + ".events.codex_die"));
        assertTrue(yaml.contains("events.codex_tf"));
        assertFalse(PlayersConfigsManager.reset(yaml, "codex_die"));

        assertTrue(PlayersConfigsManager.reset(yaml, "all"));
        assertFalse(yaml.contains("events"));
    }

    @Test
    void resetWithoutCharactersOnlyTouchesTheAccount() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("name: Steve\n");

        assertFalse(PlayersConfigsManager.reset(yaml, "all"));
    }
}
