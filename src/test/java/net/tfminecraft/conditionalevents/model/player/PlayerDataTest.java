package net.tfminecraft.conditionalevents.model.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
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
    void movingGivesTheHeirOnlyTheChosenEventsAndMerges() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setOneTime("research_bronze", true);
        data.setCooldown("research_bronze", 50);
        data.setOneTime("codex_die", true);
        data.setCooldown("global_booster", 900);
        data.setCooldown(character, "codex_die", 70);

        int moved = data.moveToCharacter(character, Set.of("research_bronze", "codex_die")::contains);

        assertEquals(2, moved);
        assertTrue(data.isModified());
        assertTrue(data.isEventOneTime(character, "research_bronze"));
        assertEquals(50, data.getCooldown(character, "research_bronze"));
        assertTrue(data.isEventOneTime(character, "codex_die"));
        assertEquals(70, data.getCooldown(character, "codex_die"));
        assertFalse(data.isEventOneTime("research_bronze"));
        assertEquals(900, data.getCooldown("global_booster"));
    }

    @Test
    void movingNothingLeavesTheAccountUnmodified() {
        PlayerData data = new PlayerData(UUID.randomUUID(), "Steve");
        data.setCooldown("global_booster", 900);

        assertEquals(0, data.moveToCharacter(character, name -> false));
        assertFalse(data.isModified());
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
