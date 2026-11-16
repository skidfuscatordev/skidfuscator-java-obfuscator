package dev.skidfuscator.obfuscator.event;

import dev.skidfuscator.obfuscator.event.annotation.Listen;
import dev.skidfuscator.obfuscator.event.impl.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic EventBus quickly mushed up together to serve as an excuse
 * to not use the visitor pattern.
 */
public class EventBus {
    // Each synchronous obfuscation runs on its caller's thread. Never share listeners
    // between concurrent sessions or retain them after a failed session.
    private static final ThreadLocal<Map<Class<?>, List<EventListener>>> listeners =
            ThreadLocal.withInitial(LinkedHashMap::new);
    private static final AtomicLong registrationOrder = new AtomicLong();
    private static final Comparator<EventListener> ORDER = Comparator
            .comparingInt(EventListener::getPriority).thenComparingLong(listener -> listener.order);

    /**
     * Registers a listener to the EventBus.
     *
     * @param instance Instance of the listener to be registered
     */
    public static void register(final Listener instance) {
        final Set<Method> methods = new LinkedHashSet<>();

        // get parent until abstract transformer is reached
        Class<?> clazz = instance.getClass();

        while (!clazz.equals(Listener.class) && !clazz.equals(Object.class)) {
            Arrays.stream(clazz.getDeclaredMethods())
                    .sorted(Comparator.comparing(Method::toGenericString))
                    .forEach(methods::add);
            clazz = clazz.getSuperclass();
        }

        for (Method declaredMethod : methods) {
            if (!declaredMethod.isAnnotationPresent(Listen.class))
                continue;

            if (!declaredMethod.isAccessible()) {
                declaredMethod.setAccessible(true);
            }

            if (declaredMethod.getParameterCount() != 1
                    || !Event.class.isAssignableFrom(declaredMethod.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Event listener must accept exactly one Event: " + declaredMethod);
            }

            final Listen listen = declaredMethod.getAnnotation(Listen.class);

            final EventListener listener = new EventListener(
                    instance,
                    declaredMethod,
                    declaredMethod.getParameterTypes()[0],
                    listen.value()
            );

            List<EventListener> cached = listeners.get().computeIfAbsent(instance.getClass(), k -> new ArrayList<>());
            cached.add(listener);
        }
    }

    /**
     * Unregisters a specific listener from the EventBus.
     *
     * @param listener the instance of the listener to be removed
     */
    public static void unregister(final Object listener) {
        listeners.get().values().forEach(value -> value.removeIf(entry -> entry.listener == listener));
        listeners.get().values().removeIf(List::isEmpty);
    }

    /**
     * Unregisters a specific class from the EventBus.
     *
     * @param listener the class of the listener to be removed
     */
    public static void unregister(final Class<?> listener) {
        listeners.get().remove(listener);
    }

    /**
     * Calls an event of type T
     *
     * @param <T>   the Type parameter of the event
     * @param event the event
     * @return Modified or intact output after passing through all the interceptors
     */
    public static <T extends Event> T call(final T event, Predicate<EventListener>... preconditions) {
        final List<EventListener> calls = new ArrayList<>();

        for (List<EventListener> value : listeners.get().values()) {
            loop:
                for (EventListener listener : value) {
                    for (Predicate<EventListener> precondition : preconditions) {
                        if (!precondition.test(listener)) continue loop;
                    }
                    if (listener.check(event)) calls.add(listener);
                }
        }

        calls.sort(ORDER);
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
        final List<EventListener> calls = new ArrayList<>();
        final Set<Class<?>> skippedSet = new LinkedHashSet<>(Arrays.asList(skipped));
        for (List<EventListener> value : listeners.get().values()) {
            for (EventListener listener : value) {
                if (skippedSet.stream().anyMatch(type -> type.isInstance(listener.listener)))
                    continue;

                if (listener.check(event)) calls.add(listener);
            }
        }

        calls.sort(ORDER);
        for (EventListener call : calls) {
            call.callUnsafe(event);
        }

        return event;
    }

    /**
     * Kills the EventBus and clears all of its listeners.
     */
    public static void end() {
        listeners.remove();
    }

    /**
     * Wrapper class for EventListener
     */
    public static class EventListener {
        private final Listener listener;
        private final Method method;
        private final Class<?> type;
        private final int priority;
        private final long order = registrationOrder.getAndIncrement();

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
         * @return Returns the listener of this event
         */
        public Listener getListener() {
            return listener;
        }

        /**
         * @return type of the object of the listener
         */
        public Class<?> getType() {
            return type;
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
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new IllegalStateException("Event listener failed: " + method, cause);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot invoke event listener: " + method, e);
            }
        }
    }
}
