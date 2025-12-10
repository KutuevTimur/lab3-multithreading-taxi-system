package taxi;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//
public class Taxi implements Runnable {

    private final int id;
    private final String name;
    private final Dispatcher dispatcher;
    private final BlockingQueue<RideRequest> personalQueue = new LinkedBlockingQueue<>();
    private final Random random = new Random();

    private volatile boolean running = true;

    // состояние такси
    private volatile TaxiStatus status = TaxiStatus.FREE;
    private volatile Point currentPosition = new Point(0, 0);

    public Taxi(int id, String name, Dispatcher dispatcher) {
        this.id = id;
        this.name = name;
        this.dispatcher = dispatcher;
    }
    public String getName() {
        return name;
    }

    public TaxiStatus getStatus() {
        return status;
    }

    public Point getCurrentPosition() {
        return currentPosition;
    }

    // пометить такси занятым, вызывается только диспетчером под его локом
    public void markBusy() {
        status = TaxiStatus.BUSY;
    }

    // пометить такси свободным.
     // вызывается диспетчером после завершения рейса.
    public void markFree() {
        status = TaxiStatus.FREE;
    }

    // остановка работы такси.
    public void stop() {
        running = false;
        Thread.currentThread().interrupt();
    }

    // назначить заказ этому такси
    // диспетчер кладёт заказ в персональную очередь такси

    public void assignRide(RideRequest request) {
        try {
            personalQueue.put(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("Не удалось назначить заказ " + request + " из-за прерывания");
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName(name);
        log("Запуск потока такси");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                log("Ожидаю новый заказ...");
                RideRequest request = personalQueue.take();
                log("Получен " + request);
                // едем к клиенту
                simulateDriveToClient(request);

                // везём клиента до точки назначения
                simulateRideWithClient(request);

                // уведомляем диспетчера
                dispatcher.onTripFinished(this, request);

            } catch (InterruptedException e) {
                log("Такси прервано, выхожу из цикла");
                Thread.currentThread().interrupt();
            }
        }

        log("Такси остановлено");
    }

    private void simulateDriveToClient(RideRequest request) throws InterruptedException {
        double distance = DistanceUtil.distance(currentPosition, request.getFrom());
        long delay = computeDelay(distance);
        log("Еду к клиенту. Расстояние: " + String.format("%.2f", distance) +
                ", задержка: " + delay + " мс");
        Thread.sleep(delay);
        currentPosition = request.getFrom();
    }

    private void simulateRideWithClient(RideRequest request) throws InterruptedException {
        double distance = DistanceUtil.distance(request.getFrom(), request.getTo());
        long delay = computeDelay(distance);
        log("Везу клиента. Расстояние: " + String.format("%.2f", distance) +
                ", задержка: " + delay + " мс");
        Thread.sleep(delay);
        currentPosition = request.getTo();
        log("Поездка завершена. Текущая позиция такси: " + currentPosition);
    }

    /**
     * Приблизительный расчёт задержки по расстоянию:
     * чем дальше, тем дольше. Добавляем немного случайности.
     */

    // примерный расчет задержки по расстоянию
    // чем дальше тем дольше, ну и чутка рандома добавляем
    private long computeDelay(double distance) {
        long base = (long) (distance * 20); // коэффициент скорости
        long noise = random.nextInt(300);   // чутка рандома
        return Math.max(200, base + noise);
    }

    private void log(String message) {
        System.out.printf("[%s] %s%n",
                Thread.currentThread().getName(), message);
    }
}
