package test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Analyzer {

    private static final String DEFAULT_ORIGIN = "VVO";
    private static final String DEFAULT_DESTINATION = "TLV";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy H:mm");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <tickets.json> [origin] [destination]");
            return;
        }

        String filePath = args[0];
        String origin = args.length > 1 ? args[1] : DEFAULT_ORIGIN;
        String destination = args.length > 2 ? args[2] : DEFAULT_DESTINATION;

        try {
            List<Ticket> tickets = loadTickets(filePath, origin, destination);

            if (tickets.isEmpty()) {
                System.out.printf("No tickets found for %s → %s%n", origin, destination);
                return;
            }

            Map<String, Duration> minTimes = getMinFlightTimes(tickets);
            PriceStats stats = getPriceStats(tickets);

            printMinTimes(minTimes, origin, destination);
            printPriceStats(stats);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static List<Ticket> loadTickets(String path, String origin, String destination) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(path));

        List<Ticket> tickets = new ArrayList<>();
        for (JsonNode node : root.get("tickets")) {
            if (!node.get("origin").asText().equals(origin) || !node.get("destination").asText().equals(destination)) {
                continue;
            }

            LocalDateTime dep = LocalDateTime.parse(
                    node.get("departure_date").asText() + " " + node.get("departure_time").asText(), FORMATTER);
            LocalDateTime arr = LocalDateTime.parse(
                    node.get("arrival_date").asText() + " " + node.get("arrival_time").asText(), FORMATTER);

            tickets.add(new Ticket(node.get("carrier").asText(), dep, arr, node.get("price").asInt()));
        }
        return tickets;
    }

    private static Map<String, Duration> getMinFlightTimes(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.carrier,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(Ticket::getDuration)),
                                opt -> opt.map(Ticket::getDuration).orElse(Duration.ZERO)
                        )
                ));
    }

    private static PriceStats getPriceStats(List<Ticket> tickets) {
        List<Integer> prices = tickets.stream().map(t -> t.price).sorted().toList();
        double avg = prices.stream().mapToInt(Integer::intValue).average().orElse(0);
        double median = prices.size() % 2 == 0
                ? (prices.get(prices.size() / 2 - 1) + prices.get(prices.size() / 2)) / 2.0
                : prices.get(prices.size() / 2);
        return new PriceStats(avg, median, Math.abs(avg - median));
    }

    private static void printMinTimes(Map<String, Duration> minTimes, String origin, String destination) {
        System.out.printf("Минимальное время полета (%s → %s):%n", origin, destination);
        minTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("%s: %dч %dм%n",
                e.getKey(), e.getValue().toHours(), e.getValue().toMinutes() % 60));
    }

    private static void printPriceStats(PriceStats stats) {
        System.out.printf("%nСредняя цена: %.2f%n", stats.avg);
        System.out.printf("Медиана: %.2f%n", stats.median);
        System.out.printf("Разница: %.2f%n", stats.diff);
    }
}
