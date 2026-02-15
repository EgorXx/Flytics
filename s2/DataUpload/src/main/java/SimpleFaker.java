import java.util.concurrent.ThreadLocalRandom;

public class SimpleFaker {
    static final String[] FIRST_NAMES = {"Ivan", "Petr", "Anna", "Maria", "Olga", "Sergey", "Dmitry", "Elena", "Alex", "John"};
    static final String[] LAST_NAMES = {"Ivanov", "Petrov", "Sidorov", "Smirnov", "Popov", "Kuznetsov", "Sokolov", "Mikhailov"};
    static final String[] MEALS = {"VEGAN", "KOSHER", "HALAL", "KIDS", "LOW_FAT"};
    static final String[] TAGS = {"VIP", "WHEELCHAIR", "WITH_INFANT", "EXTRA_BAG", "FAST_TRACK"};
    static final String[] FLIGHT_CODES = {"SU100", "SU101", "DP200", "DP201", "S7300", "S7301", "UT400", "UT401"};

    public static String name() { return FIRST_NAMES[rnd(FIRST_NAMES.length)]; }
    public static String surname() { return LAST_NAMES[rnd(LAST_NAMES.length)]; }
    public static String email(String fn, String ln, int i) { return fn.toLowerCase() + "." + ln.toLowerCase() + "." + i + "@example.com"; }

    public static String jsonContact() {
        return String.format("{\"phone\": \"+79%09d\", \"telegram\": \"@user%d\"}", rnd(1000000000), rnd(10000));
    }

    public static String[] tagsArray() {
        if (Math.random() < 0.8) return new String[]{};
        return new String[]{TAGS[rnd(TAGS.length)]};
    }

    private static int rnd(int max) { return ThreadLocalRandom.current().nextInt(max); }
}
