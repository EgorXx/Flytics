package ru.kpfu.itis.sorokin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.kpfu.itis.sorokin.dto.TaskToProcess;

import java.time.Duration;
import java.util.Optional;

@Repository
public class TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createFlightNotificationTask(
            long flightId,
            long flightEventId,
            String eventType,
            int priority
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tasks(payload, priority, status)
                VALUES (
                    jsonb_build_object(
                        'taskType', 'NOTIFY_FLIGHT_EVENT',
                        'flightId', ?,
                        'flightEventId', ?,
                        'eventType', ?
                    ),
                    ?,
                    'READY'
                )
                RETURNING id
                """,
                Long.class,
                flightId,
                flightEventId,
                eventType,
                priority
        );
    }

    public Optional<TaskToProcess> claimNextTask() {
        return jdbcTemplate.query(
                """
                WITH next_task AS (
                    SELECT id
                    FROM tasks
                    WHERE status = 'READY'
                      AND scheduled_at <= now()
                    ORDER BY priority DESC, created_at ASC
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE tasks t
                SET status = 'RUNNING',
                    attempts = attempts + 1,
                    started_at = now(),
                    updated_at = now()
                FROM next_task
                WHERE t.id = next_task.id
                RETURNING t.id, t.payload::text, t.priority, t.attempts
                """,
                (rs, rowNum) -> new TaskToProcess(
                        rs.getLong("id"),
                        rs.getString("payload"),
                        rs.getInt("priority"),
                        rs.getInt("attempts")
                )
        ).stream().findFirst();
    }

    public void markCompleted(long taskId) {
        jdbcTemplate.update(
                """
                UPDATE tasks
                SET status = 'COMPLETED',
                    completed_at = now(),
                    updated_at = now()
                WHERE id = ?
                """,
                taskId
        );
    }

    public void markRetry(long taskId, Duration delay) {
        jdbcTemplate.update(
                """
                UPDATE tasks
                SET status = 'READY',
                    scheduled_at = now() + (? * interval '1 second'),
                    updated_at = now()
                WHERE id = ?
                """,
                delay.toSeconds(),
                taskId
        );
    }

    public void markFailed(long taskId) {
        jdbcTemplate.update(
                """
                UPDATE tasks
                SET status = 'FAILED',
                    updated_at = now()
                WHERE id = ?
                """,
                taskId
        );
    }
}
