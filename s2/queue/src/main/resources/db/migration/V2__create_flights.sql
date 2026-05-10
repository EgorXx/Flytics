CREATE TABLE flights
(
    id                BIGSERIAL PRIMARY KEY,
    flight_number     VARCHAR(8)  NOT NULL,
    departure_airport VARCHAR(3)  NOT NULL,
    arrival_airport   VARCHAR(3)  NOT NULL,
    departure_time    TIMESTAMPTZ NOT NULL,
    arrival_time      TIMESTAMPTZ NOT NULL,
    status            VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_flights_status
        CHECK (status IN ('SCHEDULED', 'DELAYED', 'CANCELLED', 'COMPLETED'))
);

CREATE TABLE flight_events
(
    id         BIGSERIAL PRIMARY KEY,
    flight_id  BIGINT      NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_flight_events_flight
        FOREIGN KEY (flight_id) REFERENCES flights (id)
);

CREATE INDEX idx_flight_events_flight_id
    ON flight_events (flight_id);

INSERT INTO flights (flight_number,
                     departure_airport,
                     arrival_airport,
                     departure_time,
                     arrival_time,
                     status)
VALUES ('SU100', 'SVO', 'LED', now() + interval '2 hours', now() + interval '3 hours 30 minutes', 'SCHEDULED'),
       ('DP200', 'VKO', 'KZN', now() + interval '4 hours', now() + interval '5 hours 40 minutes', 'SCHEDULED'),
       ('U6300', 'DME', 'AER', now() + interval '1 hour', now() + interval '3 hours', 'SCHEDULED');

