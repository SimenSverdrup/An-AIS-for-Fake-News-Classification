package AIS;

import java.util.Arrays;

public class Affinity {
    public double CalculateDistance(double[] antigen_features, double[] antibody_features, boolean[] features_used) {
        // Calculates Euclidean distance between two points (antigen and antibody)

        double distance = 0;

        for (int i=0; i<antibody_features.length; i++) {
            if (features_used[i]) {
                double diff = antibody_features[i] - antigen_features[i];
                distance += Math.pow(diff, 2);
            }
        }


        return Math.sqrt((distance));
    }

    public double CalculateAffinity(double[] antigen_features, double[] antibody_features, double RR_radius, boolean[] features_used) {
        // Calculates affinity between antigen and antibody

        double euclidean_distance = CalculateDistance(antigen_features, antibody_features, features_used);
        if (euclidean_distance > RR_radius) return 0; // outside RR
        else if (euclidean_distance == 0) return 1; // ag exactly at ab coordinates
        else return 1 - euclidean_distance/RR_radius; // Affinity => 1 when euclidean distance very small, affinity => 0 when euclidean distance is barely within RR
    }
}
