package AIS;

public class Affinity {
    public double CalculateDistance(double[] antigen_features, double[] antibody_features) {
        // Calculates Euclidean distance between two points (antigen and antibody)

        double distance = 0;

        for (int i=0; i<antibody_features.length; i++) {
            double diff = antibody_features[i] - antigen_features[i];
            distance += Math.pow(diff, 2);
        }

        return Math.sqrt((distance));
    }

    public double CalculateAffinity(double[] antigen_features, double[] antibody_features, double RR_radius) {
        double euclidean_distance = CalculateDistance(antigen_features, antibody_features);
        if (euclidean_distance <= RR_radius) return 1/euclidean_distance;
        else return 0;
    }
}
