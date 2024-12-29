package dev.skidfuscator.failsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Failsafe {
    public interface Action {
        /**
         * Execute the logic associated with this action when handling an exception.
         *
         * @param builder The current failsafe builder.
         * @param e The exception that triggered this action.
         * @param attempt The current attempt count (1-based).
         * @param maxAttempts The maximum number of attempts allowed by this action.
         * @return true if done (stop), false if continue retrying.
         */
        boolean execute(FailsafeBuilder builder, Exception e, int attempt, int maxAttempts);
    }

    public static final Action RETRY = new Action() {
        @Override
        public boolean execute(FailsafeBuilder builder, Exception e, int attempt, int maxAttempts) {
            int effectiveMax = (maxAttempts > 0) ? maxAttempts : builder.defaultMaxAttempts;
            if (attempt < effectiveMax) {
                return false; // try again
            } else {
                // max attempts reached, rethrow
                throw asRuntimeException(e);
            }
        }
    };

    public static final Action CANCEL = (builder, e, attempt, maxAttempts) -> true;
    public static final Action UNDO = (builder, e, attempt, maxAttempts) -> true;

    public static FailsafeBuilder run(Runnable code) {
        return new FailsafeBuilder().run(code);
    }

    public static <T> FailsafeBuilder run(Iterable<T> values, Consumer<T> code) {
        return new FailsafeBuilder().run(values, code);
    }

    public static <T> FailsafeBuilder run(T value, Consumer<T> code) {
        return new FailsafeBuilder().run(value, code);
    }

    public static class FailsafeBuilder {
        private Runnable baseAction;
        private Runnable finalAction;
        private Runnable successAction;

        private final List<ExceptionHandler<? extends Exception>> exceptionHandlers = new ArrayList<>();
        private int defaultMaxAttempts = 1;

        private FailsafeBuilder run(Runnable code) {
            this.baseAction = code;
            return this;
        }

        private <T> FailsafeBuilder run(Iterable<T> values, Consumer<T> code) {
            this.baseAction = () -> {
                for (T val : values) {
                    code.accept(val);
                }
            };
            return this;
        }

        private <T> FailsafeBuilder run(T value, Consumer<T> code) {
            this.baseAction = () -> code.accept(value);
            return this;
        }

        public OnExceptionBuilder<Exception> onException() {
            return new OnExceptionBuilder<>(Exception.class, this);
        }

        public <E extends Exception> OnExceptionBuilder<E> onException(Class<E> exceptionType) {
            return new OnExceptionBuilder<>(exceptionType, this);
        }

        public FailsafeBuilder onSuccess(Runnable action) {
            this.successAction = action;
            return this;
        }

        public FailsafeBuilder finallyDo(Runnable action) {
            this.finalAction = action;
            return this;
        }

        public void finish() {
            if (baseAction == null) {
                throw new IllegalStateException("No base action provided. Call run(...) before execute().");
            }

            boolean success = false;
            try {
                runWithHandlers();
                success = true;
                if (successAction != null) {
                    successAction.run();
                }
            } finally {
                if (finalAction != null) {
                    finalAction.run();
                }
            }
        }

        private void runWithHandlers() {
            int attempt = 0;
            boolean done = false;

            while (!done) {
                attempt++;
                try {
                    baseAction.run();
                    done = true;
                } catch (Exception e) {
                    ExceptionHandler<? extends Exception> handler = findMatchingHandler(e);
                    if (handler == null) {
                        throw asRuntimeException(e);
                    }

                    // Apply strategy modifiers
                    handler.applyStrategy(e);

                    // Execute the action
                    boolean result = handler.getAction().execute(this, e, attempt, handler.getMaxAttempts());

                    // If exception occurred and action triggered, run the post callback if any
                    if (handler.getPostCallback() != null) {
                        handler.getPostCallback().run();
                    }

                    done = result;
                }
            }
        }

        private ExceptionHandler<? extends Exception> findMatchingHandler(Exception e) {
            for (ExceptionHandler<? extends Exception> handler : exceptionHandlers) {
                if (handler.handles(e)) {
                    return handler;
                }
            }
            return null;
        }

        void addExceptionHandler(ExceptionHandler<? extends Exception> handler) {
            exceptionHandlers.add(handler);
        }
    }

    private static RuntimeException asRuntimeException(Exception e) {
        return (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
    }

    private static class ExceptionHandler<E extends Exception> {
        private final Class<E> type;
        private final Action action;
        private final Runnable strategyModifierRunnable;
        private final Consumer<E> strategyModifierConsumer;
        private final int maxAttempts;
        private final Runnable postCallback;

        ExceptionHandler(
                Class<E> type,
                Action action,
                Runnable strategyModifierRunnable,
                Consumer<E> strategyModifierConsumer,
                int maxAttempts,
                Runnable postCallback
        ) {
            this.type = type;
            this.action = action;
            this.strategyModifierRunnable = strategyModifierRunnable;
            this.strategyModifierConsumer = strategyModifierConsumer;
            this.maxAttempts = maxAttempts;
            this.postCallback = postCallback;
        }

        boolean handles(Exception e) {
            return type.isAssignableFrom(e.getClass());
        }

        Action getAction() {
            return action;
        }

        int getMaxAttempts() {
            return maxAttempts;
        }

        Runnable getPostCallback() {
            return postCallback;
        }

        void applyStrategy(Exception e) {
            if (strategyModifierRunnable != null) {
                strategyModifierRunnable.run();
            } else if (strategyModifierConsumer != null) {
                @SuppressWarnings("unchecked")
                E castException = (E) e;
                strategyModifierConsumer.accept(castException);
            }
        }
    }

    public static class OnExceptionBuilder<E extends Exception> {
        private final Class<E> exceptionType;
        private final FailsafeBuilder parent;

        private Action pendingAction;
        private Runnable strategyModifierRunnable;
        private Consumer<E> strategyModifierConsumer;
        private int maxAttempts = -1;
        private Runnable postCallback;

        OnExceptionBuilder(Class<E> exceptionType, FailsafeBuilder parent) {
            this.exceptionType = exceptionType;
            this.parent = parent;
        }

        public OnExceptionBuilder<E> retry(int attempts) {
            this.pendingAction = RETRY;
            this.maxAttempts = attempts;
            return this;
        }

        public OnExceptionBuilder<E> cancel() {
            this.pendingAction = CANCEL;
            return this;
        }

        public OnExceptionBuilder<E> undo() {
            this.pendingAction = UNDO;
            return this;
        }

        public OnExceptionBuilder<E> modify(Consumer<E> modifier) {
            this.strategyModifierConsumer = modifier;
            return this;
        }

        public OnExceptionBuilder<E> modify(Runnable modifier) {
            this.strategyModifierRunnable = modifier;
            return this;
        }

        /**
         * Sets a callback to run if this exception handler is triggered.
         * This does not override the action. If no action was specified before,
         * defaults to CANCEL.
         */
        public FailsafeBuilder execute(Runnable callback) {
            this.postCallback = callback;
            finalizeHandler();
            return parent;
        }

        /**
         * If the user calls execute() without arguments (not requested, but we can keep it),
         * we finalize without a callback.
         */
        public FailsafeBuilder execute() {
            finalizeHandler();
            return parent;
        }

        private void finalizeHandler() {
            if (pendingAction == null) {
                // No action was set, default to CANCEL
                pendingAction = CANCEL;
            }
            ExceptionHandler<E> handler = new ExceptionHandler<>(
                    exceptionType,
                    pendingAction,
                    strategyModifierRunnable,
                    strategyModifierConsumer,
                    maxAttempts,
                    postCallback
            );
            parent.addExceptionHandler(handler);
        }
    }
}

