package net.tfminecraft.conditionalevents.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.model.CEEvent;
import net.tfminecraft.conditionalevents.model.EventType;

class EventsManagerIndexTest {

    private static CEEvent event(String name, EventType type, boolean enabled) {
        CEEvent event = new CEEvent(name);
        event.setEventType(type);
        event.setEnabled(enabled);
        return event;
    }

    @Test
    void validEventsAreThatTypesEnabledEventsInFileOrder() {
        EventsManager manager = new EventsManager(mock(ConditionalEvents.class));
        CEEvent first = event("first", EventType.ITEM_INTERACT, true);
        CEEvent command = event("command", EventType.PLAYER_COMMAND, true);
        CEEvent off = event("off", EventType.ITEM_INTERACT, false);
        CEEvent second = event("second", EventType.ITEM_INTERACT, true);
        manager.setEvents(new ArrayList<>(List.of(first, command, off, second)));

        assertEquals(List.of(first, second), manager.getValidEvents(EventType.ITEM_INTERACT));
        assertEquals(List.of(first, off, second), manager.getEventsByType(EventType.ITEM_INTERACT));
        assertEquals(List.of(command), manager.getValidEvents(EventType.PLAYER_COMMAND));
        assertTrue(manager.getValidEvents(EventType.PROTOCOLLIB_RECEIVE_MESSAGE).isEmpty());
        assertTrue(manager.getEventsByType(EventType.PROTOCOLLIB_RECEIVE_MESSAGE).isEmpty());
    }

    @Test
    void enablingAtRuntimeAndReloadingAreSeen() {
        EventsManager manager = new EventsManager(mock(ConditionalEvents.class));
        assertTrue(manager.getValidEvents(EventType.ITEM_INTERACT).isEmpty());
        CEEvent off = event("off", EventType.ITEM_INTERACT, false);
        manager.setEvents(new ArrayList<>(List.of(off)));
        assertTrue(manager.getValidEvents(EventType.ITEM_INTERACT).isEmpty());
        off.setEnabled(true);
        assertEquals(List.of(off), manager.getValidEvents(EventType.ITEM_INTERACT));

        CEEvent reloaded = event("reloaded", EventType.PLAYER_JOIN, true);
        manager.setEvents(new ArrayList<>(List.of(reloaded)));
        assertTrue(manager.getValidEvents(EventType.ITEM_INTERACT).isEmpty());
        assertEquals(List.of(reloaded), manager.getValidEvents(EventType.PLAYER_JOIN));
    }

    @Test
    void callersGetTheirOwnLists() {
        EventsManager manager = new EventsManager(mock(ConditionalEvents.class));
        CEEvent join = event("join", EventType.PLAYER_JOIN, true);
        manager.setEvents(new ArrayList<>(List.of(join)));
        ArrayList<CEEvent> copy = manager.getEventsByType(EventType.PLAYER_JOIN);
        copy.clear();
        assertEquals(List.of(join), manager.getEventsByType(EventType.PLAYER_JOIN));
        assertNotSame(manager.getValidEvents(EventType.PLAYER_JOIN), manager.getValidEvents(EventType.PLAYER_JOIN));
    }
}
