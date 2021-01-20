package Features;

import java.util.Arrays;
import java.util.List;

public class Normaliser {

    public double[][] NormaliseFeatures(double[][] feature_list) {
        // Normalises feature values between 0 and 1

        double max = feature_list[0][0];
        double min = feature_list[0][0];

        for (double[] vector : feature_list) {
            // find largest and smallest values in the 2D list
            for (double value : vector) {
                if (value > max) max = value;
                if (value < min) min = value;
            }
        }

        int i=0;
        int j=0;

        for (double[] vector : feature_list) {
            // normalises the feature values
            for (double value : vector) {
                feature_list[i][j] = (value - min)/(max - min);
                j++;
            }
            j=0;
            i++;
        }

        return feature_list; // pass by reference/value kan bli et problem her
    }
}
