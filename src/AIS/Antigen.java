package AIS;

import Dataset.Dataset;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import Dataset.Dataset;
import Features.Tokenizer;

public class Antigen {
    public String true_class;   // the true class of the antigen
    public String id;
    public String[] classes;
    public String[] fake_news_binary_classes = {"real", "fake"};
    public String[] LIAR_classes = {"pants-fire", "false", "barely-true", "half-true", "mostly-true", "true"};
    public String[] iris_classes = {"Iris-setosa", "Iris-versicolor", "Iris-virginica"};
    public String[] wine_classes = {"1", "2", "3"};
    public String[] spirals_classes = {"0", "1", "2"};
    public String[] diabetes_classes = {"0", "1"};

    public int number_of_classes;
    public String predicted_class;  // the predicted class of the antigen, after all antibodies have voted and decided
    public double[] class_vote;
    public double average_affinity;
    public double max_affinity;
    public double minimum_affinity;
    // length k, where k is the number of classes
    // the array will contain the cumulative voting score for each of the classes, the one with the highest value will be the predicted class
    public Dataset dataset;
    public int number_of_features;
    public double[] feature_list;
    public List<Antibody> connected_antibodies; // the connected antibodies
    public List<Double> affinities; // the affinities to the antibodies (must be in the same order as antibodies)
    public List<Double> sorted_affinities; // the affinities to the antibodies (in increasing order)

    public List<String> tokenized_text; // only tokenized
    public List<String> tokenized_and_partly_processed_text; // tokenized and removed unwanted
    public List<String> tokenized_and_fully_processed_text;
    public int word_count; // number of words in raw_text

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antigen(List<String> record, int number_of_features, Dataset dataset, boolean binary_class_LIAR) {
        // record is a single line in the form:
        // id,date,speaker,statement,sources,paragraph_based_content,fullText_based_content,label_fnn
        this.dataset = dataset;
        this.number_of_features = number_of_features;
        this.feature_list = new double[this.number_of_features];

        switch (this.dataset) {
            case FAKENEWSNET -> {
                this.classes = this.fake_news_binary_classes.clone();
                this.true_class = record.get(record.size() - 1).toLowerCase();
                this.id = record.get(0);

                this.speaker = record.get(2); // speaker
                this.headline = record.get(3); // headline
                this.raw_text = record.get(6); // full text
                this.sources = record.get(4).split(", ");
                this.number_of_classes = 2;
                parseSources();

                Tokenizer tokenizer = new Tokenizer();
                this.tokenized_text = tokenizer.tokenizeText(this.raw_text);
                this.tokenized_and_partly_processed_text = tokenizer.tokenizeTextAndRemoveCharacters(this.raw_text);
                this.tokenized_and_fully_processed_text = tokenizer.tokenizeAndProcessText(this.raw_text);
            }
            case LIAR -> {
                String temp = record.get(record.size() - 1).toLowerCase();
                this.true_class = temp;

                if (binary_class_LIAR) {
                    this.classes = this.fake_news_binary_classes.clone();
                    this.number_of_classes = 2;
                    switch (temp) {
                        case "true":
                            this.true_class = "real";
                        case "mostly-true":
                            this.true_class = "real";
                        case "false":
                            this.true_class = "fake";
                        case "pants-fire":
                            this.true_class = "fake";
                    // remaining labels have been taken care of in Controller
                    }
                }
                else {
                    this.classes = this.LIAR_classes;
                    this.number_of_classes = 6;
                }

                this.id = record.get(0);
                this.speaker = record.get(2); // speaker
                this.headline = record.get(3); // headline
                this.sources = record.get(4).split(", ");
                this.raw_text = record.get(6); // full text (useful for BERT?)
                parseSources();

                Tokenizer tokenizer = new Tokenizer();
                this.tokenized_text = tokenizer.tokenizeText(this.raw_text);
                this.tokenized_and_partly_processed_text = tokenizer.tokenizeTextAndRemoveCharacters(this.raw_text);
                this.tokenized_and_fully_processed_text = tokenizer.tokenizeAndProcessText(this.raw_text);
            }
            case WINE -> {
                // WINE dataset has class label at first index
                this.classes = this.wine_classes.clone();
                this.true_class = record.get(0); // FIRST index in row
                for (int index=0; index < this.number_of_features; index++) {
                    this.feature_list[index] = Double.parseDouble(record.get(index+1));
                }
                this.number_of_classes = 3;
            }
            case IRIS -> {
                this.classes = this.iris_classes.clone();
                this.true_class = record.get(this.number_of_features); // last index in row
                for (int index=0; index < this.number_of_features; index++) {
                    this.feature_list[index] = Double.parseDouble(record.get(index));
                }
                this.number_of_classes = 3;
            }
            case SPIRALS -> {
                this.classes = this.spirals_classes.clone();
                this.true_class = record.get(this.number_of_features); // last index in row
                for (int index=0; index < this.number_of_features; index++) {
                    this.feature_list[index] = Double.parseDouble(record.get(index));
                }
                this.number_of_classes = 3;
            }
            case DIABETES -> {
                this.classes = this.diabetes_classes.clone();
                this.true_class = record.get(this.number_of_features); // last index in row
                for (int index=0; index < this.number_of_features; index++) {
                    this.feature_list[index] = Double.parseDouble(record.get(index));
                }
                this.number_of_classes = 2;
            }
        }
        this.class_vote = new double[this.classes.length];
        this.connected_antibodies = new ArrayList<>();
        this.affinities = new ArrayList<>();
    }

    public void reset() {
        // Should be called between each training iteration

        this.connected_antibodies.clear();
        this.affinities.clear();
        for (int i=0; i<this.classes.length; i++) {
            this.class_vote[i] = 0;
        }
        this.predicted_class = "";
    }

    public void findConnectedAntibodies(ArrayList<Antibody> antibodies) {
        // Antibodies input argument is all the antibodies
        this.reset();

        Affinity aff = new Affinity();

        for (Antibody ab : antibodies) {
            double affinity = aff.CalculateAffinity(ab.feature_list, this.feature_list, ab.RR_radius);
            if (affinity > 0) {
                // The antibody is within the RR
                this.affinities.add(affinity);
                this.connected_antibodies.add(ab);
            }
        }

        // System.out.println("\nAntigen.findConnectedAntibodies, id: " + this.id);
        // System.out.println("Antigen.findConnectedAntibodies, connected antibodies: " + this.connected_antibodies + "\n");
    }

    public void calculateAffinities() {
        double total_aff = 0;
        double min_aff = 1;
        double max_aff = 0;
        int ab_idx = 0;

        for (Antibody connected_ab : this.connected_antibodies) {
            total_aff += this.affinities.get(ab_idx);
            if (this.affinities.get(ab_idx) > max_aff) max_aff = this.affinities.get(ab_idx);
            else if (this.affinities.get(ab_idx) < min_aff) min_aff = this.affinities.get(ab_idx);
            ab_idx++;
        }

        this.sorted_affinities = this.affinities;
        Collections.sort(this.sorted_affinities);
        this.average_affinity = total_aff/this.connected_antibodies.size();
        this.max_affinity = max_aff;
        this.minimum_affinity = min_aff;
    }

    public void predictClass(ArrayList<Antibody> antibodies) {
        // Iterate through the connected antibodies, to determine the predicted class
        // We se a voting tally, where a vote is proportional to the affinity

        int index = 0;
        Affinity aff = new Affinity();

        for (Antibody ab : this.connected_antibodies) {
            for (int i=0; i<this.classes.length; i++) {
                if (ab.true_class.equals(this.classes[i])) {
                    this.class_vote[i] += this.affinities.get(index);//*ab.weighted_accuracy;
                }
            }
            index++;
        }
        if (this.connected_antibodies.size() == 0) {
            // No antibody is connected, let the one with lowest distance/RR radius decide
            double min_ratio = 1000;
            for (Antibody ab : antibodies) {
                if ((aff.CalculateDistance(this.feature_list, ab.feature_list) / ab.RR_radius) < min_ratio) {
                    this.predicted_class = ab.true_class;
                }
            }
        }

        this.predicted_class = this.classes[this.getIndexOfLargest(this.class_vote)];
    }

    public int getIndexOfLargest(double[] array) {
        // From https://stackoverflow.com/questions/22911722/how-to-find-array-index-of-largest-value

        if (array == null || array.length == 0) return -1; // null or empty

        int largest = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest; // position of the first largest found
    }

    public void parseSources() {
        this.sources[0] = this.sources[0].substring(1);
        int length = this.sources.length;
        this.sources[length-1] = this.sources[length-1].substring(0, this.sources[length-1].length());

        for (int i=0; i<this.sources.length; i++) {
            this.sources[i] = this.sources[i].length() > 1 ? this.sources[i].substring(1, this.sources[i].length()-1) : this.sources[i];
            try {
                URI uri = new URI(this.sources[i]);
                String domain = uri.getHost();
                if (domain != null) this.sources[i] = domain.startsWith("www.") ? domain.substring(4) : domain;
                else this.sources[i] = "none";
            }
            catch (URISyntaxException err) {
                //System.out.println("Could not fetch host information");
                this.sources[i] = "none";
            }
        }
    }
}
