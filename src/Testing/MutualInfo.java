package Testing;

import AIS.Antigen;
import JavaMI.MutualInformation;

import java.util.ArrayList;
import java.util.Arrays;

public class MutualInfo {

    public double calculateMutualInformation(double[] vector1, double[] vector2) {
        return MutualInformation.calculateMutualInformation(vector1, vector2);
    }

    public void calculateTextEmbeddingMI(ArrayList<Antigen> training_antigens) {
        double[] mutual_informations = new double[training_antigens.size()];

        int count = 0;

        for (Antigen ag : training_antigens) {
            double[] feature1 = Arrays.copyOfRange(ag.feature_list, 0, 768);
            double[] feature2 = Arrays.copyOfRange(ag.feature_list, 768, ag.feature_list.length);

            mutual_informations[count] = this.calculateMutualInformation(feature1, feature2);
            count++;
        }

        double sum = 0;
        for (double val : mutual_informations) {
            sum += val;
        }

        System.out.println("Average: " + (sum/mutual_informations.length));
    }

    public void calculateSingleFeatureMI(ArrayList<Antigen> training_antigens, int number_of_features) {
        double[][] features = new double[number_of_features][training_antigens.size()];

        int count = 0;
        for (Antigen ag : training_antigens) {
            for (int u = 0; u < number_of_features; u++) {
                features[u][count] = ag.feature_list[u];
            }
            count++;
        }

        for (int u = 0; u < number_of_features; u++) {
            System.out.println("\nFeature " + (u+1) + ": ");
            for (int v = 0; v < number_of_features; v++) {
                double mutualInformation = this.calculateMutualInformation(features[u], features[v]);
                System.out.println(mutualInformation);
            }
        }
    }
}
