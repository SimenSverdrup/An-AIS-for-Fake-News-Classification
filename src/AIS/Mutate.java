package AIS;

import Features.Normaliser;

import java.util.Arrays;
import java.util.Random;

public class Mutate {
    private final double range_min_feature_list = 0.0; // the minimum value to be added/subtracted from the feature list
    private final double range_max_feature_list = 0.1; // the maximum value to be added/subtracted from the feature list


    private final double range_min_RR_radius = 0.0; // the minimum value to be added/subtracted from the RR radius
    private final double range_max_RR_radius = 0.06; // the maximum value to be added/subtracted from the RR radius


    public Mutate() {
        // Fitness is the fitness of the parent antibody
        // We want to increase chance of mutation if fitness is low
        // (probability increases if wrong class and within RR or correct class but low affinity)
        // Probability must be  a number between [0,1], but should mostly be less than 1/(1+n),
        // where n is the length of the feature vector
        // that means less than 0.33

        //this.probability = 1/fitness;
        //System.out.println(this.probability);
    }

    double[] mutateVector(double[] feature_vector, double mutation_probability) {
        // Mutates the input vector according to the probability
        // Note, the more elements in the vector (feature list), the less the vector probability should be

        int i=0;

        for (double value : feature_vector) {
            double randomValue = Math.random();  //0.0 to 0.99
            if (randomValue <= mutation_probability) {
                double randomValue2 = Math.random();  //0.0 to 0.99
                if (randomValue2 >= 0.5) {
                    // Randomly select whether to add or subtract
                    feature_vector[i] = Math.min((this.range_max_feature_list - this.range_min_feature_list) * Math.random() + feature_vector[i], 1.0);
                }
                else {
                    feature_vector[i] = Math.max(feature_vector[i] - (this.range_max_feature_list - this.range_min_feature_list) * Math.random(), 0.0);
                }
            }
            i++;
        }

        return feature_vector;
    }

    double mutateScalar(double value, double mutation_probability) {
        // Mutates a single value, made for mutating the RR radius

        double randomValue = Math.random();  //0.0 to 99.9
        if (randomValue <= mutation_probability) {
            double randomValue2 = Math.random();  //0.0 to 0.99
            double randomValue3 = (this.range_max_RR_radius - this.range_min_RR_radius) * Math.random();

            if (randomValue2 < 0.5) {
                // Randomly select whether to add or subtract from value
                value = Math.min(randomValue3 + value, 1.0);
            }
            else {
                value = Math.max(value - randomValue3, 0.0);
            }
        }

        return value;
    }
}
