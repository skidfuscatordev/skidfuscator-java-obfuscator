package dev.skidfuscator.discord;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

public class ObfuscatorQueue extends LinkedBlockingQueue<ObfuscatorRequest> {
    private final Consumer<ObfuscatorRequest> poller;

    public ObfuscatorQueue(Consumer<ObfuscatorRequest> poller) {
        this.poller = poller;
    }

    @Override
    public boolean add(ObfuscatorRequest obfuscatorRequest) {
        if (isEmpty()) {
            kewl(obfuscatorRequest);
            return true;
        } else {
            return super.add(obfuscatorRequest);
        }
    }

    private void kewl(final ObfuscatorRequest obfuscatorRequest) {
        CompletableFuture.runAsync(() -> {
            try {
                poller.accept(obfuscatorRequest);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            super.remove(obfuscatorRequest);

            if (!isEmpty()) {
                kewl(peek());
            }
        });
    }

    @Override
    public boolean offer(ObfuscatorRequest obfuscatorRequest) {
        final boolean valid = super.offer(obfuscatorRequest);
        return valid;
    }

    @Override
    public ObfuscatorRequest poll() {
        return super.poll();
    }

    @Override
    public ObfuscatorRequest take() throws InterruptedException {
        return super.take();
    }

    @Override
    public ObfuscatorRequest peek() {
        return super.peek();
    }

    @Override
    public boolean remove(Object o) {
        return super.remove(o);
    }
}
