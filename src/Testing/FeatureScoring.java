package Testing;

import AIS.Antigen;
import Dataset.Dataset;
import net.sf.javaml.featureselection.scoring.GainRatio;
import net.sf.javaml.tools.data.FileHandler;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FeatureScoring {
    public Dataset dataset;
    public int number_of_features;

    public FeatureScoring(Dataset dataset, int number_of_features) {
        this.dataset = dataset;
        this.number_of_features = number_of_features;
    }

    public void outputDataset(ArrayList<Antigen> training_antigens) {
        // Write the extracted features and classes to .data file

        String file_name = switch (this.dataset) {
            case KAGGLE -> "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\kaggle.data";
            case LIAR -> "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\liar.data";
            case FAKENEWSNET -> "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\fnn.data";
            default -> throw new IllegalStateException("Unexpected value: " + this.dataset);
        };

        try {
            FileWriter myWriter = new FileWriter(file_name);
            for (Antigen ag : training_antigens) {
                for (double val : ag.feature_list) {
                    myWriter.write(val + ",");
                }
                myWriter.write(ag.true_class + "\n");
            }
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to file.");
            e.printStackTrace();
        }
    }

    public void getFeatureScores() throws IOException {

        net.sf.javaml.core.Dataset data = switch (this.dataset) {
            case IRIS -> FileHandler.loadDataset(new File("Datasets/iris.data"), 4, ",");
            case KAGGLE -> FileHandler.loadDataset(new File("Datasets/kaggle.data"), number_of_features, ",");
            case LIAR -> FileHandler.loadDataset(new File("Datasets/liar.data"), number_of_features, ",");
            case FAKENEWSNET -> FileHandler.loadDataset(new File("Datasets/fnn.data"), number_of_features, ",");
            default -> throw new IllegalStateException("Unexpected value: " + this.dataset);
        };

        GainRatio ga = new GainRatio();

        ga.build(data);

        for (int j = 0; j < ga.noAttributes(); j++) {
            System.out.println(ga.score(j));
        }
    }
}
