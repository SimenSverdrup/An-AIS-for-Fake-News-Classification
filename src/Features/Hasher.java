package Features;

public class Hasher {
    private final int hashTableSize = 100;

    public double StringToHash(String str) {
        double hash = Math.abs(str.hashCode() % hashTableSize);

        return hash/100;
    }
}
