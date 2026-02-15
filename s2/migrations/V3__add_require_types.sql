ALTER TABLE booking
    ADD COLUMN booking_token UUID DEFAULT gen_random_uuid();

-- Детали заказа
ALTER TABLE booking
    ADD COLUMN contact_data JSONB DEFAULT '{}';

ALTER TABLE booking
    ADD COLUMN user_location POINT;

ALTER TABLE flight
    ADD COLUMN flight_time_range tstzrange;

UPDATE flight
SET flight_time_range = tstzrange(departure_time, arrival_time);


-- Список особенностей пассажиров
ALTER TABLE passenger
    ADD COLUMN tags TEXT[];

-- ФИО для быстрого поиска
ALTER TABLE passenger
    ADD COLUMN search_vector tsvector;


-- прошла ли онлайн регистрация
ALTER TABLE ticket
    ADD COLUMN is_online_checkin BOOLEAN DEFAULT FALSE;


ALTER TABLE flight
    ADD COLUMN actual_arrival_time TIMESTAMPTZ;

-- Выбор по пище, если нет, то NULL
ALTER TABLE ticket
    ADD COLUMN meal_choice VARCHAR(20);
