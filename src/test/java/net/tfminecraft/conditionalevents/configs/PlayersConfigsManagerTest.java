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
        assertEquals(null, loaded.getSeededTo());

        data.seed(character, name -> false);
        YamlConfiguration seeded = new YamlConfiguration();
        PlayersConfigsManager.write(data.snapshot(), seeded);
        assertEquals(character, PlayersConfigsManager.read(account, seeded).getSeededTo());
        seeded.set("seeded-to", "nonsense");
        assertEquals(null, PlayersConfigsManager.read(account, seeded).getSeededTo());
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

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path dataFolder;

    @Test
    void aFailedSaveKeepsTheDataModifiedAndASuccessfulOneIsAtomic() throws Exception {
        net.tfminecraft.conditionalevents.ConditionalEvents plugin = org.mockito.Mockito.mock(net.tfminecraft.conditionalevents.ConditionalEvents.class);
        org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        org.mockito.Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        PlayersConfigsManager manager = new PlayersConfigsManager(plugin, "players");
        java.nio.file.Files.createDirectories(dataFolder.resolve("players"));
        PlayerData data = new PlayerData(account, "Steve");
        data.seed(character, name -> false);
        java.nio.file.Path blocker = dataFolder.resolve("players").resolve(account + ".yml.tmp");
        java.nio.file.Files.createDirectories(blocker);

        assertFalse(manager.saveConfig(data));
        assertTrue(data.isModified());

        java.nio.file.Files.delete(blocker);
        assertTrue(manager.saveConfig(data));
        assertFalse(data.isModified());
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(dataFolder.resolve("players").resolve(account + ".yml").toFile());
        assertEquals(character.toString(), saved.getString("seeded-to"));
    }

    @Test
    void resetAllLeavesFilesOfLoadedAccountsToTheirNextSave() throws Exception {
        net.tfminecraft.conditionalevents.ConditionalEvents plugin = org.mockito.Mockito.mock(net.tfminecraft.conditionalevents.ConditionalEvents.class);
        org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        org.mockito.Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        net.tfminecraft.conditionalevents.managers.PlayerManager players = new net.tfminecraft.conditionalevents.managers.PlayerManager(plugin);
        org.mockito.Mockito.when(plugin.getPlayerManager()).thenReturn(players);
        PlayersConfigsManager manager = new PlayersConfigsManager(plugin, "players");
        java.nio.file.Path folder = java.nio.file.Files.createDirectories(dataFolder.resolve("players"));
        UUID offline = UUID.randomUUID();
        String done = "name: X\nevents:\n  codex_die:\n    one_time: true\n";
        java.nio.file.Files.writeString(folder.resolve(account + ".yml"), done);
        java.nio.file.Files.writeString(folder.resolve(offline + ".yml"), done);
        java.nio.file.Files.writeString(folder.resolve("notes.yml"), done);
        java.nio.file.Files.writeString(folder.resolve(UUID.randomUUID() + ".yml"), "name: Y\n");
        players.addPlayer(new PlayerData(account, "Steve"));

        manager.resetDataForAllPlayers("codex_die");

        assertTrue(java.nio.file.Files.readString(folder.resolve(account + ".yml")).contains("codex_die"));
        assertFalse(java.nio.file.Files.readString(folder.resolve(offline + ".yml")).contains("codex_die"));
        assertTrue(java.nio.file.Files.readString(folder.resolve("notes.yml")).contains("codex_die"));
        assertTrue(manager.filesLock() != null);

        java.nio.file.Files.createDirectories(folder.resolve(offline + ".yml.tmp"));
        java.nio.file.Files.writeString(folder.resolve(offline + ".yml"), done);
        manager.resetDataForAllPlayers("codex_die");
        assertTrue(java.nio.file.Files.readString(folder.resolve(offline + ".yml")).contains("codex_die"));

        java.nio.file.Files.delete(folder.resolve(offline + ".yml.tmp"));
        org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.resolve("missing").toFile());
        manager.resetDataForAllPlayers("codex_die");
    }
}
