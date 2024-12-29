package dev.skidfuscator.obfuscator.util;

import java.util.HashSet;
import java.util.Set;

public interface Observable<T> {
    T get();

    void set(T value);

    void addObserver(Observer<T> observer);

    void removeObserver(Observer<T> observer);

    class SimpleObservable<T> implements Observable<T> {
        private final Set<Observer<T>> observers = new HashSet<>();
        private T value;

        public SimpleObservable(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
            observers.forEach(observer -> observer.onChanged(value));
        }

        @Override
        public void addObserver(Observer<T> observer) {
            observers.add(observer);
        }

        @Override
        public void removeObserver(Observer<T> observer) {
            observers.remove(observer);
        }
    }

    class MergedBooleanObservable implements Observable<Boolean> {
        private final Set<Observable<Boolean>> observables = new HashSet<>();
        private final Set<Observer<Boolean>> observers = new HashSet<>();

        public MergedBooleanObservable(Observable<Boolean>... observables) {
            for (Observable<Boolean> observable : observables) {
                this.observables.add(observable);
                observable.addObserver(value -> observers.forEach(observer -> observer.onChanged(get())));
            }
        }

        public Boolean get() {
            return observables.stream().allMatch(Observable::get);
        }

        public void set(Boolean value) {
            observables.forEach(observable -> observable.set(value));
        }

        @Override
        public void addObserver(Observer<Boolean> observer) {
            observers.add(observer);
        }

        @Override
        public void removeObserver(Observer<Boolean> observer) {
            observers.remove(observer);
        }
    }

    interface Observer<T> {
        void onChanged(T value);
    }
}
