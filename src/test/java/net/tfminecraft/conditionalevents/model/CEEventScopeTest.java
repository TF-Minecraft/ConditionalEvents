package net.tfminecraft.conditionalevents.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CEEventScopeTest {
    @Test
    void characterIsTheDefaultButOnlyMattersForEventsWithData() {
        CEEvent tip = new CEEvent("join_tip");
        assertTrue(tip.isCharacterScoped());
        assertFalse(tip.usesCharacterData());

        CEEvent unlock = new CEEvent("codex_die");
        unlock.setOneTime(true);
        assertTrue(unlock.usesCharacterData());

        CEEvent meditate = new CEEvent("meditate");
        meditate.setCooldown(10);
        assertTrue(meditate.usesCharacterData());

        meditate.setCharacterScoped(false);
        assertFalse(meditate.usesCharacterData());
    }
}
