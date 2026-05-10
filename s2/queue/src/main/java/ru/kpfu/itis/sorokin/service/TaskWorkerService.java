package ru.kpfu.itis.sorokin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kpfu.itis.sorokin.dto.TaskToProcess;
import ru.kpfu.itis.sorokin.repository.TaskRepository;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TaskWorkerService {

    private static final Logger log = LoggerFactory.getLogger(TaskWorkerService.class);

    private final TaskRepository taskRepository;

    public TaskWorkerService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public boolean processOne(String workerName) {
        var taskOptional = taskRepository.claimNextTask();

        if (taskOptional.isEmpty()) {
            return false;
        }

        TaskToProcess task = taskOptional.get();

        try {
            log.info(
                    "Worker {} started task id={}, priority={}, attempts={}, payload={}",
                    workerName,
                    task.id(),
                    task.priority(),
                    task.attempts(),
                    task.payload()
            );

            simulateProcessing();

            taskRepository.markCompleted(task.id());

            log.info("Worker {} completed task id={}", workerName, task.id());
            return true;
        } catch (Exception e) {
            if (task.attempts() >= 3) {
                taskRepository.markFailed(task.id());
                log.warn("Worker {} failed task id={} permanently", workerName, task.id(), e);
            } else {
                taskRepository.markRetry(task.id(), Duration.ofSeconds(5));
                log.warn("Worker {} returned task id={} to retry", workerName, task.id(), e);
            }

            return true;
        }
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(100, 500));

            if (ThreadLocalRandom.current().nextInt(100) < 10) {
                throw new RuntimeException("Random processing error");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Worker interrupted", e);
        }
    }
}
