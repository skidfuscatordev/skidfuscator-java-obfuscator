package dev.skidfuscator.obfuscator.event;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class EventBus {
    private static final Map<Class<?>, List<EventListener>> listeners = new HashMap<>();

    public static void register(final Listener instance) {
        for (Method declaredMethod : instance.getClass().getDeclaredMethods()) {
            if (!declaredMethod.isAnnotationPresent(Listen.class))
                continue;

            if (!declaredMethod.isAccessible()) {
                declaredMethod.setAccessible(true);
            }

            if (declaredMethod.getParameterCount() > 1) {
                throw new IllegalStateException("Event listener listening to... more that one thing?");
            }

            final Listen listen = declaredMethod.getAnnotation(Listen.class);

            final EventListener listener = new EventListener(
                    instance,
                    declaredMethod,
                    declaredMethod.getParameterTypes()[0],
                    listen.value()
            );

            List<EventListener> cached = listeners.computeIfAbsent(instance.getClass(), k -> new ArrayList<>());
            cached.add(listener);
        }
    }

    public static void unregister(final Object listener) {
        unregister(listener.getClass());
    }

    public static void unregister(final Class<?> listener) {
        listeners.remove(listener);
    }

    public static <T extends Event> T call(final T event) {
        final Queue<EventListener> calls = new PriorityQueue<>(Comparator.comparingInt(EventListener::getPriority));

        for (List<EventListener> value : listeners.values()) {
            for (EventListener listener : value) {
                if (listener.check(event)) calls.add(listener);
            }
        }

        for (EventListener call : calls) {
            call.callUnsafe(event);
        }

        return event;
    }

    public static void end() {
        listeners.clear();
    }

    static class EventListener {
        private final Listener listener;
        private final Method method;
        private final Class<?> type;
        private final int priority;

        public EventListener(Listener listener, Method method, Class<?> type, int priority) {
            this.listener = listener;
            this.method = method;
            this.type = type;
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        boolean check(final Object event) {
            return type.isAssignableFrom(event.getClass());
        }

        void call(final Object event) {
            if (!check(event))
                return;

            callUnsafe(event);
        }

        @Deprecated
        void callUnsafe(final Object event) {
            try {
                method.invoke(listener, event);
            } catch (InvocationTargetException | IllegalAccessException e) {
                // TODO: Proper exception tracking
                e.printStackTrace();
            }
        }
    }
}
