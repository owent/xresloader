package org.xresloader.core;

import java.util.concurrent.CompletableFuture;

/** Conversion runs concurrently; observable file writes retain stdin order. */
public final class TaskOrder {
    private static final ThreadLocal<TaskOrder> current = new ThreadLocal<>();
    private final CompletableFuture<Void> previous;
    private final CompletableFuture<Void> complete = new CompletableFuture<>();

    TaskOrder(TaskOrder previous) {
        this.previous = previous == null ? CompletableFuture.completedFuture(null) : previous.complete;
    }

    void enter() {
        current.set(this);
    }

    public static void awaitOutputTurn() {
        TaskOrder order = current.get();
        if (order != null) {
            order.previous.join();
        }
    }

    void finish() {
        // Even a failed task must not let its successor overtake earlier writers.
        previous.join();
        complete.complete(null);
        current.remove();
    }
}
