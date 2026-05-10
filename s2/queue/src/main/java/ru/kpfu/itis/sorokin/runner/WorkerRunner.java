package ru.kpfu.itis.sorokin.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.kpfu.itis.sorokin.service.TaskWorkerService;

@Component
public class WorkerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkerRunner.class);

    private final TaskWorkerService taskWorkerService;
    private final String appMode;
    private final String workerName;

    public WorkerRunner(
            TaskWorkerService taskWorkerService,
            @Value("${app.mode}") String appMode,
            @Value("${worker.name:worker}") String workerName
    ) {
        this.taskWorkerService = taskWorkerService;
        this.appMode = appMode;
        this.workerName = workerName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"worker".equalsIgnoreCase(appMode)) {
            return;
        }

        log.info("Worker started. name={}", workerName);

        while (true) {
            boolean processed = taskWorkerService.processOne(workerName);

            if (!processed) {
                sleep(500);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Worker interrupted", e);
        }
    }
}
