INSERT INTO passenger (first_name, last_name, birthdate, passport_series, passport_number)
VALUES
    ('Ivan',  'Petrov',  '1990-05-15', '1234', '567890'),
    ('Maria', 'Ivanova', '1985-11-20', '2345', '678901')
    ON CONFLICT (passport_series, passport_number) DO NOTHING;

INSERT INTO client (first_name, last_name, email, password_hash)
VALUES
    ('Ivan', 'Petrov', 'ivan@example.com', 'new_hash_ivan')
    ON CONFLICT (email) DO UPDATE
                               SET first_name   = EXCLUDED.first_name,
                               last_name    = EXCLUDED.last_name,
                               password_hash = EXCLUDED.password_hash;

INSERT INTO fare_class (description)
VALUES ('Economy'), ('Business'), ('First')
    ON CONFLICT (description) DO NOTHING;

INSERT INTO flight_status (description)
VALUES ('On time'), ('Delayed'), ('Landed'), ('Cancelled')
    ON CONFLICT (description) DO NOTHING;