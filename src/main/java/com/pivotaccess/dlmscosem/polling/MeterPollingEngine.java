package com.pivotaccess.dlmscosem.polling;

import com.pivotaccess.dlmscosem.polling.data.MeterReading;

import java.util.Map;
import java.util.concurrent.*;

public class MeterPollingEngine {

    // Parallel executor for tasks that can run concurrently (e.g., different ports or TCP connections)
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(50);

    // Queues for strict sequential execution per connection
    private final Map<String, ExecutorService> sequentialQueue = new ConcurrentHashMap<>();

    /**
     * Routes the task to the correct hardware queue and returns a non-blocking CompletableFuture.
     */
    public CompletableFuture<MeterReading> submitTask(MeterReadTask task) {
        ExecutorService targetExecutor = !task.isParallel()
                ? parallelExecutor
                : sequentialQueue.computeIfAbsent(task.getTransportId(), k -> Executors.newSingleThreadExecutor());

        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                MeterReading err = new MeterReading(task.getMeterId(), task.getTransportId());
                err.setErrorMsg("Task execution failed: " + e.getMessage());
                return err;
            }
        }, targetExecutor);
    }

    public void shutdown() {
        parallelExecutor.shutdown();
        sequentialQueue.values().forEach(ExecutorService::shutdown);
    }
}