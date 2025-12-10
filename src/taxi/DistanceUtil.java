package taxi;


// утилита для расчета расстояния между двумя точками
public final class DistanceUtil {

    private DistanceUtil() {
    }

    public static double distance(Point a, Point b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
