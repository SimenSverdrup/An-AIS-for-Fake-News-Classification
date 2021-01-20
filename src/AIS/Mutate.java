package AIS;

import java.util.Random;

public class Mutate {
    private final double rangeMin = 0.1;
    private final double rangeMax = 2;

    double CalculateProbabilityOfMutation(double affinity, boolean correctClass) {
        return 4.5;
    }

    double[] MutateVector(double[] feature_vector, double probability) {
        // Mutates the input vector according to the probability
        // (probability increases if wrong class and within RR or correct class but low affinity)
        // Probability must be passed as a number between [0,1], but should mostly be less than 1/(1+n),
        // where n is the length of the feature vector

        int i=0;

        for (double value : feature_vector) {
            double randomValue = Math.random() * 100;  //0.0 to 99.9
            if (randomValue <= probability * 100) {
                double randomCoefficient = this.rangeMin + (this.rangeMax - this.rangeMin) * Math.random();
                feature_vector[i] = feature_vector[i] * randomCoefficient;
            }
            i++;
        }

        return feature_vector;
    }

    double MutateScalar(double value, double probability) {
        // Mutates a single value, made for mutating the RR radius

        double randomValue = Math.random() * 100;  //0.0 to 99.9
        if (randomValue <= probability * 100) {
            double randomCoefficient = this.rangeMin + (this.rangeMax - this.rangeMin) * Math.random();
            value = value * randomCoefficient;
        }
        return value;
    }
}
