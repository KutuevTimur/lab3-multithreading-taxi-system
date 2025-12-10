package taxi;


import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Диспетчер :)
// следит за очередью, выбор свободного такси, решение кому давать заказ

public class Dispatcher implements Runnable {

    // очередь для новых запросов от клиентов
    private final BlockingQueue<RideRequest> requestsQueue;

    // все такси
    private final List<Taxi> taxis;

    // для синхронизации доступа к списку такси
    private final Lock taxisLock = new ReentrantLock();

    // флаг, когда завершать
    private volatile boolean running = true;

    public Dispatcher(BlockingQueue<RideRequest> requestsQueue, List<Taxi> taxis) {
        this.requestsQueue = requestsQueue;
        this.taxis = taxis;
    }

    // метод остановки диспетчера
    public void stop() {
        running = false;
        // прерываем потко чтоб он вышел из очереди
        Thread.currentThread().interrupt();
    }

    // метод для завершения поездки таксы
    // и помечает таксу свободной
    public void onTripFinished(Taxi taxi, RideRequest request) {
        log("Получено уведомление: " + taxi.getName() +
                " завершило " + request);
        taxisLock.lock();
        try {
            taxi.markFree();
        } finally {
            taxisLock.unlock();
        }
    }

    @Override
    public void run() {
        log("Запуск диспетчера");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // ждём новый заказ из очереди
                RideRequest request = requestsQueue.poll(1, TimeUnit.SECONDS);
                if (request == null) {
                    // нет новых заказов -> просто крутимся дальше
                    continue;
                }

                log("Новый заказ в очереди: " + request);

                Taxi chosenTaxi = chooseTaxiFor(request);
                if (chosenTaxi == null) {
                    log("Нет свободных такси для " + request + ". Заказ возвращён в очередь");
                    // если нет свободных вернём заказ обратно
                    requestsQueue.put(request);
                    Thread.sleep(500);
                } else {
                    log("Назначен " + chosenTaxi.getName() + " на " + request);
                    chosenTaxi.assignRide(request);
                }

            } catch (InterruptedException e) {
                log("Диспетчер прерван, завершаю работу");
                Thread.currentThread().interrupt();
            }
        }

        log("Диспетчер остановлен");
    }

    // выбор свбодного такси которое ближе всего к клиенту
    private Taxi chooseTaxiFor(RideRequest request) {
        taxisLock.lock();
        try {
            Taxi bestTaxi = null;
            double bestDistance = Double.MAX_VALUE;

            for (Taxi taxi : taxis) {
                if (taxi.getStatus() == TaxiStatus.FREE) {
                    double distance = DistanceUtil.distance(taxi.getCurrentPosition(), request.getFrom());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestTaxi = taxi;
                    }
                }
            }

            if (bestTaxi != null) {
                // помечаем занятым, чтоб не попало в другой поток по брацки
                bestTaxi.markBusy();
            }

            return bestTaxi;
        } finally {
            // освбождаем лок
            taxisLock.unlock();
        }
    }

    // метод для логирования, чтоб видеть че делает диспетчер
    private void log(String message) {
        System.out.printf("[%s][Dispatcher] %s%n",
                Thread.currentThread().getName(), message);
    }
}
