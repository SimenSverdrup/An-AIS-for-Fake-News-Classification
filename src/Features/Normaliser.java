package Features;

import AIS.Antigen;

import java.util.Arrays;
import java.util.List;

public class Normaliser {

    public Antigen[] NormaliseFeatures(Antigen[] antigens) {
        // Normalises feature values between 0 and 1
        // The reasoning behind having max and min vectors instead of scalars,
        // is that we wish to normalise across single features at a time, not mix the features together

        double[] max_vector = antigens[0].feature_list.clone(); // will hold the maximal values at index j for every feature_list[i]
        double[] min_vector = antigens[0].feature_list.clone(); // will hold the minimal values at index j for every feature_list[i]

        for (Antigen ag : antigens) {
            // find largest and smallest values in the 2D list
            for (int j = 0; j < ag.number_of_features; j++) {
                if (ag.feature_list[j] > max_vector[j]) max_vector[j] = ag.feature_list[j];
                if (ag.feature_list[j] < min_vector[j]) min_vector[j] = ag.feature_list[j];
            }
        }

        int j=0;

        for (Antigen ag : antigens) {
            // normalises the feature values
            for (double value : ag.feature_list) {
                ag.feature_list[j] = (value - min_vector[j])/(max_vector[j] - min_vector[j]);
                j++;
            }
            j=0;
        }

        return antigens; // pass by reference/value kan bli et problem her
    }
}
