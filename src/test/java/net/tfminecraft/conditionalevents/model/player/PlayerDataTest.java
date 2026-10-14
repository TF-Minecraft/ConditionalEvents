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
        assertTrue(data.getCharacterEventData().isEmpty());
    }
}
