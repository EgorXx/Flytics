package ru.kpfu.itis.sorokin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlightRepository {

    private final JdbcTemplate jdbcTemplate;

    public FlightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long findAnyFlightId() {
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM flights
                LIMIT 1
                """,
                Long.class
        );
    }
}
