package dev.skidfuscator.test;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.Listener;
import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class EventBusTest {
    static class Trace extends Event {
        final List<String> calls = new ArrayList<>();
        Trace() { super(null); }
    }
    static class Priorities implements Listener {
        @Listen(10) public void a(Trace event) { event.calls.add("10"); }
        @Listen(100) public void b(Trace event) { event.calls.add("100"); }
        @Listen(50) public void c(Trace event) { event.calls.add("50"); }
        @Listen(20) public void d(Trace event) { event.calls.add("20"); }
    }
    static class Named implements Listener {
        final String name;
        Named(String name) { this.name = name; }
        @Listen public void on(Trace event) { event.calls.add(name); }
    }
    static class Other extends Named { Other(String name) { super(name); } }

    @AfterEach void clear() { EventBus.end(); }

    @Test void dispatchesInPriorityOrderInBothPaths() {
        EventBus.register(new Priorities());
        assertEquals(List.of("10", "20", "50", "100"), EventBus.call(new Trace()).calls);
        assertEquals(List.of("10", "20", "50", "100"), EventBus.callButSkip(new Trace()).calls);
        assertTrue(EventBus.callButSkip(new Trace(), Priorities.class).calls.isEmpty());
    }

    @Test void tiesFollowRegistrationOrderAndUnregisterRemovesOnlyOneInstance() {
        Named first = new Named("first");
        EventBus.register(first);
        EventBus.register(new Other("second"));
        EventBus.register(new Named("third"));
        assertEquals(List.of("first", "second", "third"), EventBus.call(new Trace()).calls);
        assertEquals(List.of("first", "third"), EventBus.callButSkip(new Trace(), Other.class).calls);
        EventBus.unregister(first);
        assertEquals(List.of("second", "third"), EventBus.call(new Trace()).calls);
    }

    @Test void filteringOneMethodDoesNotSkipOtherMethodsOfSameListener() {
        EventBus.register(new Priorities());
        assertEquals(List.of("10", "20", "50"), EventBus.call(new Trace(),
                listener -> listener.getPriority() != 100).calls);
    }

    @Test void propagatesListenerFailures() {
        IllegalStateException expected = new IllegalStateException("transform failed");
        EventBus.register(new Listener() {
            @Listen public void on(Trace event) { throw expected; }
        });
        assertSame(expected, assertThrows(IllegalStateException.class, () -> EventBus.call(new Trace())));
    }

    @Test void isolatesConcurrentSessions() throws Exception {
        EventBus.register(new Named("main"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertEquals(List.of("worker"), executor.submit(() -> {
                EventBus.register(new Named("worker"));
                try { return EventBus.call(new Trace()).calls; }
                finally { EventBus.end(); }
            }).get(5, TimeUnit.SECONDS));
            assertEquals(List.of("main"), EventBus.call(new Trace()).calls);
        } finally { executor.shutdownNow(); }
    }

    @Test void failedObfuscationClearsListeners() {
        Skidfuscator skid = new Skidfuscator(SkidfuscatorSession.builder().analytics(false).build()) {
            @Override public void _importConfig() {
                EventBus.register(new Named("leaked"));
                throw new IllegalStateException("invalid configuration");
            }
        };
        assertThrows(IllegalStateException.class, skid::run);
        assertTrue(EventBus.call(new Trace()).calls.isEmpty());
    }
}
