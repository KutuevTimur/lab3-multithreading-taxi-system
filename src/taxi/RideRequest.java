package taxi;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

// класс заявки клиента
public class RideRequest {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    private final int id;
    private final Point from;
    private final Point to;
    private final Instant createdAt;

    public RideRequest(Point from, Point to) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.from = from;
        this.to = to;
        this.createdAt = Instant.now();
    }

    public int getId() {
        return id;
    }

    public Point getFrom() {
        return from;
    }

    public Point getTo() {
        return to;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Order#" + id + " from " + from + " to " + to +
                " at " + createdAt;
    }
}
