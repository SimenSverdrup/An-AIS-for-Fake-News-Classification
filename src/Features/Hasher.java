package Features;

public class Hasher {
    private final int hashTableSize = 100;

    public double StringToHash(String str) {
        // Note, this hashes a single string, must be called for each source

        double hash = Math.abs(str.hashCode() % hashTableSize);

        return hash/100;
    }
}
