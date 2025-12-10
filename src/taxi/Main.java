package taxi;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        // очередь заказов потокобезопасная
        BlockingQueue<RideRequest> requestsQueue = new LinkedBlockingQueue<>();

        // Список такси
        List<Taxi> taxis = new ArrayList<>();

        // создаём диспетчера
        Dispatcher dispatcher = new Dispatcher(requestsQueue, taxis);
        Thread dispatcherThread = new Thread(dispatcher, "DispatcherThread");

        // создаём несколько такси
        int taxiCount = 3; // тут можн менять кол-во таксишек
        for (int i = 1; i <= taxiCount; i++) {
            Taxi taxi = new Taxi(i, "Taxi-" + i, dispatcher);
            taxis.add(taxi);
        }

        // создаём и запускаем потоки такси
        List<Thread> taxiThreads = new ArrayList<>();
        for (Taxi taxi : taxis) {
            Thread t = new Thread(taxi, taxi.getName());
            taxiThreads.add(t);
            t.start();
        }

        // генератор клиентов
        ClientGenerator generator = new ClientGenerator(requestsQueue);
        Thread generatorThread = new Thread(generator, "ClientGeneratorThread");

        // запускаем диспетчера и генератор клиентов
        dispatcherThread.start();
        generatorThread.start();

        // время работы симуляции (20 сек) (тоже можно менять)
        Thread.sleep(20_000);

        System.out.println("=== Инициирована остановка системы ===");

        // останавливаем генератор
        generator.stop();
        generatorThread.interrupt();

        // останавливаем диспетчера
        dispatcher.stop();
        dispatcherThread.interrupt();

        // останавливаем такси
        for (Taxi taxi : taxis) {
            taxi.stop();
        }
        for (Thread t : taxiThreads) {
            t.interrupt();
        }

        // дожидаемся завершения потоков
        try {
            generatorThread.join();
        } catch (InterruptedException ignored) {}

        try {
            dispatcherThread.join();
        } catch (InterruptedException ignored) {}

        for (Thread t : taxiThreads) {
            try {
                t.join();
            } catch (InterruptedException ignored) {}
        }

        System.out.println("=== Симуляция завершена ===");
    }
}
