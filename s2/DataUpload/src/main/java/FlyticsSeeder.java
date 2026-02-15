import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public class FlyticsSeeder {
    private static final String URL = "jdbc:postgresql://localhost:5432/flytics";
    private static final String USER = "postgres";
    private static final String PASS = "qwerty007";

    private static final int COUNT_CLIENTS = 250_000;
    private static final int COUNT_FLIGHTS = 250_000;
    private static final int COUNT_BOOKINGS = 250_000;
    private static final int COUNT_TICKETS = 250_000;
    private static final int COUNT_PASSENGERS = 250_000;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);

            System.out.println("Database cleaning");
            clearDataBase(conn);

            System.out.println("1. Inserting Clients (250k)...");
            List<Integer> clientIds = seedClients(conn, COUNT_CLIENTS);

            System.out.println("2. Inserting Flights (250k)...");
            List<Integer> flightIds = seedFlights(conn, COUNT_FLIGHTS);

            System.out.println("2.5 Creating Dummy Fare...");
            int dummyFareId = createDummyFare(conn, flightIds.get(0));

            System.out.println("3. Inserting Bookings (250k)...");
            List<Integer> bookingIds = seedBookings(conn, clientIds, COUNT_BOOKINGS);

            System.out.println("4. Inserting Passengers (250k)...");
            List<Integer> passengerIds = seedPassengers(conn, COUNT_PASSENGERS);

            System.out.println("5. Inserting Tickets (250k)...");
            seedTickets(conn, bookingIds, passengerIds, flightIds, dummyFareId);

            conn.commit();
            System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000 + "s");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void clearDataBase(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE flight, client, booking, passenger, ticket, fare RESTART IDENTITY CASCADE");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int createDummyFare(Connection conn, int flightId) throws SQLException {
        String sql = "INSERT INTO fare (flight_id, fare_class_id, price, available_seats) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            ps.setInt(2, 1);
            ps.setInt(3, 5000);
            ps.setInt(4, 1000000);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
    }



    // CLIENT Высокая кардинальность email
    private static List<Integer> seedClients(Connection conn, int count) throws SQLException {
        String sql = "INSERT INTO client (first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?) RETURNING id";
        List<Integer> ids = new ArrayList<>(count);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String fn = SimpleFaker.name();
                String ln = SimpleFaker.surname();
                ps.setString(1, fn);
                ps.setString(2, ln);
                ps.setString(3, SimpleFaker.email(fn, ln, i));
                ps.setString(4, "hash_secret_" + i);
                ps.addBatch();

                if (i % 5000 == 0) ps.executeBatch();
            }
            ps.executeBatch();


            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM client")) {
                while(rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }


    // FLIGHT Диапазоны + наллы
    private static List<Integer> seedFlights(Connection conn, int count) throws SQLException {
        String sql = "INSERT INTO flight (flight_number, aircraft_id, departure_time, arrival_time, status_id, flight_time_range, actual_arrival_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Integer> ids = new ArrayList<>();
        int[] aircraftIds = {1, 2, 3, 4, 5};

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                LocalDateTime dep = LocalDateTime.now()
                        .minusDays(ThreadLocalRandom.current().nextInt(365))
                        .plusDays(ThreadLocalRandom.current().nextInt(365))
                        .withHour(ThreadLocalRandom.current().nextInt(24))
                        .withMinute(ThreadLocalRandom.current().nextInt(60));

                dep = dep.plusMinutes(i % 100);

                LocalDateTime arr = dep.plusHours(2);

                ps.setString(1, SimpleFaker.FLIGHT_CODES[ThreadLocalRandom.current().nextInt(8)]);
                ps.setInt(2, aircraftIds[ThreadLocalRandom.current().nextInt(5)]);
                ps.setTimestamp(3, Timestamp.valueOf(dep));
                ps.setTimestamp(4, Timestamp.valueOf(arr));
                ps.setInt(5, 1);

                // диапазоны
                ps.setObject(6, String.format("[%s, %s)", dep, arr), Types.OTHER);

                // NULL-значения (20% рейсов без времени прибытия)
                if (Math.random() < 0.2) {
                    ps.setNull(7, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(7, Timestamp.valueOf(arr));
                }

                ps.addBatch();
                if (i % 2000 == 0) ps.executeBatch();
            }

            ps.executeBatch();

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM flight")) {
                while(rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }


    // BOOKING JSONB + высокая кардинальность UUID + Skew перекос данных + геометрические
    private static List<Integer> seedBookings(Connection conn, List<Integer> clientIds, int count) throws SQLException {
        String sql = "INSERT INTO booking (client_id, booking_date, total_cost, status_id, booking_token, contact_data, user_location) VALUES (?, ?, ?, ?, ?::uuid, ?::jsonb, ?)";
        List<Integer> ids = new ArrayList<>(count);

        // Перекос данных
        // 10% клиентов делают 70% всех заказов
        int topCount = (int)(clientIds.size() * 0.1);

        if (clientIds.isEmpty()) return ids;

        List<Integer> topClients = clientIds.subList(0, topCount);
        List<Integer> rareClients = clientIds.subList(topCount, clientIds.size());

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                // Реализация перекоса
                Integer clientId;
                if (Math.random() < 0.7 && !topClients.isEmpty()) {
                    clientId = topClients.get(ThreadLocalRandom.current().nextInt(topClients.size()));
                } else if (!rareClients.isEmpty()) {
                    clientId = rareClients.get(ThreadLocalRandom.current().nextInt(rareClients.size()));
                } else {
                    clientId = clientIds.get(ThreadLocalRandom.current().nextInt(clientIds.size()));
                }

                ps.setInt(1, clientId);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(30))));
                ps.setInt(3, ThreadLocalRandom.current().nextInt(5000, 50000));
                ps.setInt(4, 2);

                // Высокая кардинальность
                ps.setString(5, UUID.randomUUID().toString());

                // JSONB
                ps.setString(6, SimpleFaker.jsonContact());

                // Геометрические типы (POINT)
                // Генерируем случайную точку (широта, долгота)
                double lat = 40 + ThreadLocalRandom.current().nextDouble() * 20;
                double lon = 30 + ThreadLocalRandom.current().nextDouble() * 50;

                ps.setObject(7, String.format(Locale.US, "(%.4f, %.4f)", lon, lat), Types.OTHER);

                ps.addBatch();
                if (i % 5000 == 0) ps.executeBatch();
            }
            ps.executeBatch();

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM booking")) {
                while(rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    // PASSENGER (Массивы + tsvector)
    private static List<Integer> seedPassengers(Connection conn, int count) throws SQLException {
        String sql = "INSERT INTO passenger (first_name, last_name, birthdate, passport_series, passport_number, tags, search_vector) VALUES (?, ?, ?, ?, ?, ?, to_tsvector(?)) RETURNING id";
        List<Integer> ids = new ArrayList<>(count);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String fn = SimpleFaker.name();
                String ln = SimpleFaker.surname();

                ps.setString(1, fn);
                ps.setString(2, ln);
                ps.setDate(3, java.sql.Date.valueOf(LocalDate.of(1980 + ThreadLocalRandom.current().nextInt(20), 1, 1)));
                ps.setString(4, String.valueOf(1000 + ThreadLocalRandom.current().nextInt(8999)));
                ps.setString(5, String.format("%06d", i));

                //Массивы
                Array tagsArr = conn.createArrayOf("text", SimpleFaker.tagsArray());
                ps.setArray(6, tagsArr);

                // Полнотекстовый поиск (tsvector)
                ps.setString(7, fn + " " + ln + " " + i);

                ps.addBatch();
                if (i % 5000 == 0) ps.executeBatch();
            }
            ps.executeBatch();

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM passenger")) {
                while(rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    // TICKET Низкая селективность + наллы
    private static void seedTickets(Connection conn, List<Integer> bookingIds, List<Integer> passIds, List<Integer> flightIds, int dummyFareId) throws SQLException {
        String sql = "INSERT INTO ticket (seat_number, booking_id, passenger_id, fare_id, flight_id, is_online_checkin, meal_choice) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < bookingIds.size(); i++) {
                if (i >= passIds.size() || i >= flightIds.size()) break;

                ps.setString(1, "1A");
                ps.setInt(2, bookingIds.get(i));
                ps.setInt(3, passIds.get(i));
                ps.setInt(4, dummyFareId);

                // Берем рейс строго по порядку i
                ps.setInt(5, flightIds.get(i));

                // Низкая селективность
                // 95% регистраций = false, 5% = true
                ps.setBoolean(6, Math.random() < 0.05);

                // NULL-значения (80% NULL)
                if (Math.random() < 0.8) {
                    ps.setNull(7, Types.VARCHAR);
                } else {
                    ps.setString(7, SimpleFaker.MEALS[ThreadLocalRandom.current().nextInt(SimpleFaker.MEALS.length)]);
                }

                ps.addBatch();
                if (i % 5000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }


}