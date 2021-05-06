package Features;

import AIS.Antigen;
import java.util.ArrayList;

public class Normaliser {

    public ArrayList<Antigen> NormaliseFeatures(ArrayList<Antigen> antigens, boolean negative_vals) {
        // Normalises feature values with unit variance (Z-score normalization)
        // The reasoning behind having avg std vectors instead of scalars,
        // is that we wish to normalise across single features at a time, not mix the features together
/*
        int number_of_features = antigens.get(0).number_of_features;
        double[] average_vector = new double[number_of_features];

        for (Antigen ag : antigens) {
            for (int j = 0; j < ag.number_of_features; j++) {
                average_vector[j] += ag.feature_list[j];
            }
        }

        for (int j = 0; j < number_of_features; j++) {
            average_vector[j] = average_vector[j]/antigens.size();
        }

        double[] sum = new double[number_of_features];

        for (Antigen ag : antigens) {
            for (int j = 0; j < ag.number_of_features; j++) {
                sum[j] += Math.pow(ag.feature_list[j] - average_vector[j], 2);
            }
        }

        double[] std = new double[number_of_features];

        for (int j = 0; j < number_of_features; j++) {
            std[j] = Math.sqrt(sum[j]/(antigens.size()));
        }

        for (Antigen ag : antigens) {
            for (int j = 0; j < number_of_features; j++) {
                ag.feature_list[j] = (ag.feature_list[j] - average_vector[j]) / std[j];
            }
        }

        return antigens;
*/


        // Min-max- normalization

        double[] max_vector = antigens.get(0).feature_list.clone(); // will hold the maximal values at index j for every feature_list[i]
        double[] min_vector = antigens.get(0).feature_list.clone(); // will hold the minimal values at index j for every feature_list[i]

        if (negative_vals) {
            for (Antigen ag : antigens) {
                // find largest and smallest values in the 2D list
                for (int j = 0; j < ag.number_of_features; j++) {
                    if (ag.feature_list[j] < min_vector[j]) min_vector[j] = ag.feature_list[j];
                }
            }
            for (Antigen ag : antigens) {
                for (int j = 0; j < ag.number_of_features; j++) {
                    ag.feature_list[j] += Math.abs(min_vector[j]);
                }
            }

            min_vector = antigens.get(0).feature_list.clone();
        }

        for (Antigen ag : antigens) {
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

        return antigens;
    }
}
