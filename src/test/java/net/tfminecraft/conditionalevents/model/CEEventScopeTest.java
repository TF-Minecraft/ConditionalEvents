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

    @Test
    void onlyPerCharacterOneTimeEventsShareTheirFlag() {
        CEEvent duchy = new CEEvent("sf_title_enter_1");
        duchy.setShareOneTime(true);
        assertFalse(duchy.sharesOneTime(), "Without one_time there is nothing to share");
        duchy.setOneTime(true);
        assertTrue(duchy.sharesOneTime());
        duchy.setCharacterScoped(false);
        assertFalse(duchy.sharesOneTime(), "Account data is shared already");

        CEEvent unlock = new CEEvent("codex_die");
        unlock.setOneTime(true);
        assertFalse(unlock.sharesOneTime());
    }
}
