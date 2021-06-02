package AIS;

import java.util.Random;

public class Mutate {
    private final double range_min = 0.1; // the minimum coefficient to be multiplied with original value
    private final double range_max = 2.0; // the maximum coefficient to be multiplied with original value
    private final double max_add = 0.8; // the maximum value to be added/subtracted

    public Mutate() {
        // Fitness is the fitness of the parent antibody
        // We want to increase chance of mutation if fitness is low
        // (probability increases if wrong class and within RR or correct class but low affinity)
        // Probability must be  a number between [0,1], but should mostly be less than 1/(1+n),
        // where n is the length of the feature vector
        // that means less than 0.33
    }

    boolean[] mutateFeatureSubset(boolean[] features_used, double mutation_probability) {
        // Mutates the input vector according to the probability

        for (int i=0; i<features_used.length; i++) {
            double random = Math.random();
            if (random < mutation_probability) features_used[i] = !features_used[i]; // flip bit
        }

        return features_used;


    }

    double[] mutateVector(double[] feature_vector, double mutation_probability, boolean[] features_used) {
        // Mutates the input vector according to the probability
        // Note, the more elements in the vector (feature list), the less the vector probability should be
        Random rand = new Random();

        int i=0;

        for (double value : feature_vector) {
            if (features_used[i]) {
                // If the antibody considers this feature value
                double randomValue = rand.nextDouble(); //0.0 to 0.99
                if (randomValue <= mutation_probability) {
                    //double number = max_add * rand.nextDouble();
                    //double randomValue2 = rand.nextDouble();
                    //if (randomValue2 > 0.5) feature_vector[i] = feature_vector[i] - number;
                    //else feature_vector[i] = feature_vector[i] + number;

                    double coeff = range_min + (range_max - range_min) * rand.nextDouble();
                    if (value * coeff > 1.0) feature_vector[i] = 1.0;
                    else feature_vector[i] = Math.max(value * coeff, 0.0);
                }
            }
            i++;
        }

        return feature_vector;
    }

    double mutateScalar(double value, double mutation_probability) {
        // Mutates a single value, made for mutating the RR radius
        Random rand = new Random();
        double randomValue = rand.nextDouble(); //0.0 to 0.99

        if (randomValue <= mutation_probability) {
            //double number = max_add * rand.nextDouble();
            //double randomValue2 = rand.nextDouble();
            //if (randomValue2 > 0.5) value = value - number;
            //else value = value + number;

            double coeff = range_min + (range_max - range_min) * rand.nextDouble();

            return value*coeff;
        }

        return value;
    }
}
