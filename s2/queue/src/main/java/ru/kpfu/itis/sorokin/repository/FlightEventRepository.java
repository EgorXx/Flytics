package ru.kpfu.itis.sorokin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlightEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public FlightEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long flightId, String eventType) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO flight_events(flight_id, event_type)
                VALUES (?, ?)
                RETURNING id
                """,
                Long.class,
                flightId,
                eventType
        );
    }
}
