package AIS;

import java.util.Arrays;

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
        // Affinity => 1 when euclidean distance very small, affinity => 0 when euclidean distance is barely within RR
        double euclidean_distance = CalculateDistance(antigen_features, antibody_features);
        if (RR_radius == 0) return 0;
        else if (euclidean_distance <= RR_radius) return 1 - (euclidean_distance / (1 + euclidean_distance)); //(1 - euclidean_distance/RR_radius);
            // This affinity calculation is made by me
        else return 0;
    }
}
