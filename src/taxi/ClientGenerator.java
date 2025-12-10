package taxi;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

// ГЕНЕРАТОР КЛИЕНТОВ
// поток имитирует людей в городе, котрые вызывают такси
// он создает новые заказы и кладет в общую очередь

public class ClientGenerator implements Runnable {
    // очередб в кототорую отправляются новые заявки
    private final BlockingQueue<RideRequest> requestsQueue;
    private final Random random = new Random();

    // Флаг для завершения потока
    private volatile boolean running = true;

    public ClientGenerator(BlockingQueue<RideRequest> requestsQueue) {
        this.requestsQueue = requestsQueue;
    }
    // метод остановки генератора
    // вызываем в main если нужно завершиться
    public void stop() {
        running = false;
        Thread.currentThread().interrupt();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("ClientGenerator");
        log("Запуск генератора клиентов");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // создаем новый заказ
                RideRequest request = generateRandomRequest();

                // кладем его в потоко безопасную очередь
                requestsQueue.put(request);
                log("Сгенерирован новый заказ: " + request);

                // пауза, чтоб не так часто летели заказы
                long delay = 500 + random.nextInt(1500);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // если поток прервали -> выходим из цикла
                log("Генератор клиентов прерван");
                Thread.currentThread().interrupt();
            }
        }

        log("Генератор клиентов остановлен");
    }

    // создает заявку на поездку с рандомными коорд-ми
    private RideRequest generateRandomRequest() {
        Point from = randomPoint();
        Point to = randomPoint();
        // коорд отправления и назначения должны отличаться
        while (from.getX() == to.getX() && from.getY() == to.getY()) {
            to = randomPoint();
        }
        return new RideRequest(from, to);
    }

    // рандомная точка на карте 100 на 100
    private Point randomPoint() {
        int x = random.nextInt(100);
        int y = random.nextInt(100);
        return new Point(x, y);
    }

    // для логирования хорошего с именем потока
    private void log(String message) {
        System.out.printf("[%s] %s%n",
                Thread.currentThread().getName(), message);
    }
}
