package net.tfminecraft.conditionalevents.model.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerDataTest {
    private final UUID character = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void accountAndCharacterDataAreSeparate() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setOneTime("codex_die", true);
        data.setCooldown(character, "meditate", 100);

        assertTrue(data.isEventOneTime("codex_die"));
        assertFalse(data.isEventOneTime(character, "codex_die"));
        assertEquals(100, data.getCooldown(character, "meditate"));
        assertEquals(0, data.getCooldown("meditate"));
        assertEquals(0, data.getCooldown(other, "meditate"));
    }

    @Test
    void resetsCoverEveryCharacter() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setOneTime("codex_die", true);
        data.setOneTime(character, "codex_die", true);
        data.setOneTime(character, "codex_tf", true);

        data.resetEvent("codex_die");
        assertFalse(data.isEventOneTime("codex_die"));
        assertFalse(data.isEventOneTime(character, "codex_die"));
        assertTrue(data.isEventOneTime(character, "codex_tf"));

        data.resetAll();
        assertFalse(data.isEventOneTime(character, "codex_tf"));
        assertTrue(data.snapshot().characters().isEmpty());
    }

    @Test
    void aChangeDuringASaveKeepsTheDataModified() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setOneTime(character, "research_bronze", true);
        PlayerData.Snapshot saving = data.snapshot();

        data.setOneTime(character, "research_iron", true);
        data.markSaved(saving.revision());

        assertTrue(data.isModified());
        assertFalse(saving.characters().get(character).stream().anyMatch(e -> e.getName().equals("research_iron")));
        data.markSaved(data.snapshot().revision());
        assertFalse(data.isModified());
    }

    @Test
    void snapshotsAreCopies() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setCooldown("global_booster", 900);
        PlayerData.Snapshot snapshot = data.snapshot();

        data.setCooldown("global_booster", 1000);

        assertEquals(900, snapshot.account().get(0).getCooldown());
        assertEquals("Steve", snapshot.name());
        data.setModified(false);
        assertFalse(data.isModified());
        data.setModified(true);
        assertTrue(data.isModified());
    }
}
