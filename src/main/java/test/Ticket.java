package test;

import java.time.Duration;
import java.time.LocalDateTime;

public class Ticket {

    String carrier;
    LocalDateTime dep;
    LocalDateTime arr;
    int price;

    Ticket(String carrier, LocalDateTime dep, LocalDateTime arr, int price) {
        this.carrier = carrier;
        this.dep = dep;
        this.arr = arr;
        this.price = price;
    }

    Duration getDuration() {
        return Duration.between(dep, arr);
    }

}
