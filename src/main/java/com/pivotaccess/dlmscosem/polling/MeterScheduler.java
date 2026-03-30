package com.pivotaccess.dlmscosem.polling;

import com.pivotaccess.dlmscosem.polling.data.MeterReading;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class MeterScheduler {
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);
    private final MeterPollingEngine engine;
    private final Consumer<MeterReading> resultCallback;

    public MeterScheduler(MeterPollingEngine engine, Consumer<MeterReading> resultCallback) {
        this.engine = engine;
        this.resultCallback = resultCallback;
    }

    /**
     * Schedules a meter to be read continuously at a fixed interval.
     */
    public void schedule(MeterReadTask task, long interval, TimeUnit unit) {
        timer.scheduleAtFixedRate(() -> {

            engine.submitTask(task)
                    .orTimeout(20, TimeUnit.MINUTES) // Abort if the task hangs for max 20 minutes
                    .whenComplete((reading, throwable) -> {
                        if (throwable != null) {
                            MeterReading timeoutErr = new MeterReading(task.getMeterId(), task.getTransportId());
                            timeoutErr.setErrorMsg("Timeout or critical failure: " + throwable.getMessage());
                            resultCallback.accept(timeoutErr);
                        } else {
                            resultCallback.accept(reading);
                        }
                    });

        }, 0, interval, unit); // Start immediately, repeat every 'interval'
    }

    public void shutdown() {
        timer.shutdown();
        engine.shutdown();
    }
}