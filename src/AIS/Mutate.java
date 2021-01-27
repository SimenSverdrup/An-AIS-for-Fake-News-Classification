package AIS;

import Features.Normaliser;
import java.util.Random;

public class Mutate {
    private final double rangeMin = 0.1;
    private final double rangeMax = 2;
    public final double probability = 0.33;

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

    double[] mutateVector(double[] feature_vector) {
        // Mutates the input vector according to the probability

        int i=0;

        for (double value : feature_vector) {
            double randomValue = Math.random() * 100;  //0.0 to 99.9
            if (randomValue <= this.probability * 100) {
                double randomCoefficient = this.rangeMin + (this.rangeMax - this.rangeMin) * Math.random();
                feature_vector[i] = Math.min(feature_vector[i] * randomCoefficient, 1.0);
            }
            i++;
        }

        return feature_vector;
    }

    double mutateScalar(double value) {
        // Mutates a single value, made for mutating the RR radius

        double randomValue = Math.random() * 100;  //0.0 to 99.9
        if (randomValue <= this.probability * 100) {
            double randomCoefficient = this.rangeMin + (this.rangeMax - this.rangeMin) * Math.random();
            value = value * randomCoefficient;
        }
        return value;
    }
}
