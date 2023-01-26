package dev.skidfuscator.obfuscator.event;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Basic EventBus quickly mushed up together to serve as an excuse
 * to not use the visitor pattern.
 */
public class EventBus {
    private static final Map<Class<?>, List<EventListener>> listeners = new HashMap<>();

    /**
     * Registers a listener to the EventBus.
     *
     * @param instance Instance of the listener to be registered
     */
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

    /**
     * Unregisters a specific listener from the EventBus.
     *
     * @param listener the instance of the listener to be removed
     */
    public static void unregister(final Object listener) {
        unregister(listener.getClass());
    }

    /**
     * Unregisters a specific class from the EventBus.
     *
     * @param listener the class of the listener to be removed
     */
    public static void unregister(final Class<?> listener) {
        listeners.remove(listener);
    }

    /**
     * Calls an event of type T
     *
     * @param <T>   the Type parameter of the event
     * @param event the event
     * @return Modified or intact output after passing through all the interceptors
     */
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

    /**
     * Calls an event of type T
     *
     * @param <T>   the Type parameter of the event
     * @param event the event
     * @return Modified or intact output after passing through all the interceptors
     */
    public static <T extends Event> T callButSkip(final T event, final Class<?>... skipped) {
        final Queue<EventListener> calls = new PriorityQueue<>(Comparator.comparingInt(EventListener::getPriority));
        final Set<Class<?>> skippedSet = new HashSet<>(Arrays.asList(skipped));
        for (List<EventListener> value : listeners.values()) {
            for (EventListener listener : value) {
                if (skippedSet.contains(listener.method.getDeclaringClass()))
                    continue;

                if (listener.check(event)) calls.add(listener);
            }
        }

        for (EventListener call : calls) {
            call.callUnsafe(event);
        }

        return event;
    }

    /**
     * Kills the EventBus and clears all of its listeners.
     */
    public static void end() {
        listeners.clear();
    }

    /**
     * Wrapper class for EventListener
     */
    static class EventListener {
        private final Listener listener;
        private final Method method;
        private final Class<?> type;
        private final int priority;

        /**
         * Instantiates a new Event listener.
         *
         * @param listener the listener
         * @param method   the method
         * @param type     the type
         * @param priority the priority
         */
        public EventListener(Listener listener, Method method, Class<?> type, int priority) {
            this.listener = listener;
            this.method = method;
            this.type = type;
            this.priority = priority;
        }

        /**
         * @return the priority of the listener
         */
        public int getPriority() {
            return priority;
        }

        /**
         * @param event Object of an event
         * @return Returns a boolean of whether the listener can process the event
         */
        boolean check(final Object event) {
            return type.isInstance(event);
        }

        /**
         * Calls a specific event
         *
         * @param event Object of the event
         */
        void call(final Object event) {
            if (!check(event))
                return;

            callUnsafe(event);
        }

        /**
         * Calls an event using unsafe casting and reflections.
         *
         * @param event the object of the event
         */
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
