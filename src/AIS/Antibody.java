package AIS;


import java.util.ArrayList;
import java.util.List;

public class Antibody {
    public String true_class;
    public String id;
    public int number_of_classes = 2;   // number of classes
    public double RR_radius;
    public double[] feature_list; // note, length not equal to raw_list - as these are the floating feature values
    public List<Antigen> connected_antigens; // the antigens which this antibody is connected to (within RR)
    public List<Double> affinities; // the affinities to the antigens (must be in the same order as antigens)
    public double fitness;
    double correct_AG_interactions;

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antibody(Antigen antigen) {
        // record is a single line in the form:
        // id,date,speaker,statement,sources,paragraph_based_content,fullText_based_content,label_fnn
        this.true_class = antigen.true_class;
        this.id = antigen.id;
        this.speaker = antigen.speaker;
        this.headline = antigen.headline;
        this.raw_text = antigen.raw_text;
        this.sources = antigen.sources;
        this.feature_list = antigen.feature_list;

        this.connected_antigens = new ArrayList<>();
        this.affinities = new ArrayList<>();
    }

    public void reset() {
        // Resets the antibody, should be called between learning iterations
        this.connected_antigens.clear();
        this.affinities.clear();
        this.fitness = 0;
    }

    public void findConnectedAntigens(Antigen[] antigens) {
        // Antigens input argument is all the antigens
        Affinity aff = new Affinity();

        for (Antigen ag : antigens) {
            double temp = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius);
            if (temp > 0) {
                // The antigen is within the RR
                this.affinities.add(temp);
                this.connected_antigens.add(ag);
            }
        }
    }

    public double calculateFitness(Antigen[] antigens) {
        // Antigens input argument is all the antigens
        // Fitness function from MAIM and VALIS
        // F(b) = SharingFactor*WeightedAccuracy/AG_interactions

        this.findConnectedAntigens(antigens);

        Affinity aff = new Affinity();
        this.correct_AG_interactions = 0;
        double AG_interactions = 0;
        double sharing_factor = 0;

        for (double affinity : this.affinities) {
            // Sum up all the affinities to connected antigens
            AG_interactions += affinity;
        }

        for (Antigen ag : this.connected_antigens) {
            // Sum up all the affinities to connected antigens with the same class
            // Note, this antibody votes for the same class - for all connected antigens (true_class of antibody)
            double temp = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius);

            if (ag.true_class.equals(this.true_class)) this.correct_AG_interactions += temp;

            // Calculate the interaction share and add to the sharing factor
            double ag_affinities = 0;
            for (double affinity : ag.affinities) {
                ag_affinities += affinity;
            }

            sharing_factor += Math.pow(temp, 2)/ag_affinities;
        }

        double weighted_accuracy = (1 + this.correct_AG_interactions)/(this.number_of_classes + AG_interactions);  // Apply Laplacian smoothing

        this.fitness = sharing_factor*weighted_accuracy/AG_interactions;

        System.out.println("AG_interactions: " + AG_interactions);
        System.out.println("correct_AG_interactions: " + this.correct_AG_interactions);
        System.out.println("weighted_accuracy: " + weighted_accuracy);
        System.out.println("sharing_factor: " + sharing_factor);
        System.out.println("Fitness: " + this.fitness);

        return this.fitness;
    }

    public void mutate() {
        Mutate mut = new Mutate();
        this.RR_radius = mut.mutateScalar(this.RR_radius);
        this.feature_list = mut.mutateVector(this.feature_list);
    }

    public void test() {
        assert(this.true_class.equals("fake") || this.true_class.equals("true"));
    }
}
