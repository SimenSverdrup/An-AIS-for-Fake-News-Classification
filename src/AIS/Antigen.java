package AIS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Antigen {
    public String true_class;   // the true class of the antigen
    public String id;
    public String[] classes = {"real", "fake"};
    public String predicted_class;  // the predicted class of the antigen, after all antibodies have voted and decided
    public double[] class_vote;
    // length k, where k is the number of classes
    // the array will contain the cumulative voting score for each of the classes, the one with the highest value will be the predicted class

    public int number_of_features;
    public double[] feature_list;
    public List<Antibody> connected_antibodies; // the connected antibodies
    public List<Double> affinities; // the affinities to the antibodies (must be in the same order as antibodies)


    public String[] tokenizedText;
    public double TF;
    public double TFIDF;
    public int word_count; // number of words in raw_text

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antigen(List<String> record, int number_of_features) {
        // record is a single line in the form:
        // id,date,speaker,statement,sources,paragraph_based_content,fullText_based_content,label_fnn

        this.number_of_features = number_of_features;
        this.feature_list = new double[this.number_of_features];
        this.true_class = record.get(record.size() - 1).toLowerCase();
        this.id = record.get(0);

        this.speaker = record.get(2); // speaker
        this.headline = record.get(3); // headline
        this.raw_text = record.get(6); // full text
        this.sources = record.get(4).split(", ");

        this.connected_antibodies = new ArrayList<>();
        this.affinities = new ArrayList<>();

        this.class_vote = new double[this.classes.length];

        parseSources();
        tokenizeText();
    }

    public void reset() {
        // Should be called between each training iteration

        if (this.connected_antibodies != null) {
            this.connected_antibodies.clear();
            this.affinities.clear();
        }
    }

    public void findConnectedAntibodies(Antibody[] antibodies) {
        // Antibodies input argument is all the antibodies
        this.reset();

        Affinity aff = new Affinity();

        for (Antibody ab : antibodies) {
            double temp = aff.CalculateAffinity(ab.feature_list, this.feature_list, ab.RR_radius);
            if (temp > 0) {
                // The antigen is within the RR
                this.affinities.add(temp);
                this.connected_antibodies.add(ab);
            }
        }

        //System.out.println("\nAntigen.findConnectedAntibodies, id: " + this.id);
        //System.out.println("Antigen.findConnectedAntibodies, connected antibodies: " + this.connected_antibodies + "\n");
    }

    public void predictClass() {
        // Iterate through the connected antibodies, to determine the predicted class
        // We se a voting tally, where a vote is proportional to the affinity

        int index = 0;

        for (Antibody ab : this.connected_antibodies) {
            if (ab.true_class.equals(this.classes[0])) {
                // Class == real
                this.class_vote[0] += this.affinities.get(index);
            }
            else if (ab.true_class.equals(this.classes[1])) {
                // Class == fake
                this.class_vote[1] += this.affinities.get(index);
            }
            index++;
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


    public void tokenizeText() {
        String temp = this.raw_text.replaceAll("'", ""); // replace
        temp = temp.replaceAll("\\W+", " "); // replace all non-letter characters

        this.tokenizedText = temp.split(" ");

        int i = 0;

        for (String word : this.tokenizedText) {
            this.tokenizedText[i] = word.toLowerCase();
            i++;
        }
        this.word_count = this.tokenizedText.length;
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
