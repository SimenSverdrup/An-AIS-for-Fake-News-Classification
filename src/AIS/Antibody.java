package AIS;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Antibody {
    public String true_class;
    public String id;
    public int number_of_classes = 2;   // number of classes
    public double RR_radius;
    public double[] feature_list; // note, length not equal to raw_list - as these are the floating feature values
    public List<Antigen> connected_antigens; // the antigens which this antibody is connected to (within RR)
    public List<Double> affinities; // the affinities to the antigens (must be in the same order as antigens)
    public double fitness;
    public double correct_AG_interactions;
    public double single_aff; // the affinity to a single antigen

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antibody(Antigen antigen) {
        // Constructor taking an antigen input

        this.true_class = antigen.true_class;
        this.id = antigen.id;
        this.speaker = antigen.speaker;
        this.headline = antigen.headline;
        this.raw_text = antigen.raw_text;
        this.sources = antigen.sources.clone();
        this.feature_list = antigen.feature_list.clone();

        this.connected_antigens = new ArrayList<>();
        this.affinities = new ArrayList<>();
    }

    public Antibody(Antibody antibody) {
        // Constructor taking another antibody as input

        this.true_class = antibody.true_class;
        this.id = antibody.id;
        this.speaker = antibody.speaker;
        this.headline = antibody.headline;
        this.raw_text = antibody.raw_text;
        this.sources = antibody.sources.clone();
        this.feature_list = antibody.feature_list.clone();
        this.RR_radius = antibody.RR_radius;
        this.number_of_classes = antibody.number_of_classes;
        this.true_class = antibody.true_class;
        this.fitness = antibody.fitness;
        this.affinities = antibody.affinities;
        this.connected_antigens = antibody.connected_antigens;
    }

    public void reset() {
        // Resets the antibody, should be called between learning iterations
        if (this.connected_antigens != null) {
            this.connected_antigens.clear();
            this.affinities.clear();
        }
        this.fitness = 0;
    }

    public void findConnectedAntigens(ArrayList<Antigen> antigens) {
        // Antigens input argument is all the antigens

        this.reset();

        Affinity aff = new Affinity();

        this.connected_antigens = new ArrayList<>();
        this.affinities = new ArrayList<>();

        for (Antigen ag : antigens) {
            double temp = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius);
            if (temp > 0) {
                // The antigen is within the RR
                this.affinities.add(temp);
                this.connected_antigens.add(ag);
            }
        }
    }

    public void calculateFitness(ArrayList<Antigen> antigens) {
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
            for (double ag_aff : ag.affinities) {
                // Note, the antigen's affinities (not this antibody's affinities)
                ag_affinities += ag_aff;
            }

            if (ag_affinities > 0) sharing_factor += Math.pow(temp, 2)/ag_affinities;
            // Sharing factor is large when few antibodies are connected to the antigen, this reward being one of few antibodies to connect to the antigen
        }

        double weighted_accuracy = (1 + this.correct_AG_interactions)/(this.number_of_classes + AG_interactions);  // Apply Laplacian smoothing

        if (AG_interactions > 0) {
            this.fitness = Math.max((sharing_factor*weighted_accuracy/AG_interactions)-(this.RR_radius*0.01), 0.0); //TODO OBS OBS tvinger fram liten RR radius her
        }
        else {
            this.fitness = 0;
        }
        /*
        System.out.println("\nAG_interactions: " + AG_interactions + "\nSharing factor: " + sharing_factor);
        System.out.println("Weighted acc: " + weighted_accuracy + "\nthis.correct_AG_interactions: " + this.correct_AG_interactions);
        System.out.println("Fitness: " + this.fitness);*/
    }

    public void random() {
        // Initialises the antibody randomly
        this.reset();

        for (int idx=0; idx<this.feature_list.length; idx++) {
            this.feature_list[idx] = Math.random();
        }
        this.RR_radius = Math.random()*0.15; // TODO: NOTE, should probably be lower when more features
        this.id = String.valueOf((int) (Math.random()*10000));

        double rnd = Math.random();
        if (rnd < 0.5) this.true_class = "real";
        else this.true_class = "fake";
    }

    public void mutate(double vector_mutation_prob, double scalar_mutation_prob) {
        Mutate mut = new Mutate();
        this.id = String.valueOf( (int) Math.floor(Math.random()*10000));
        this.RR_radius = mut.mutateScalar(this.RR_radius, scalar_mutation_prob);
        this.feature_list = mut.mutateVector(this.feature_list, vector_mutation_prob);
    }

    public boolean equals(Antibody other_ab) {
        return this.id.equals(other_ab.id);
    }

    public void calculateAffinity(Antigen ag) {
        Affinity aff = new Affinity();

        this.single_aff = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius);
    }

    public double calculateAffinity(Antibody other_ab) {
        Affinity aff = new Affinity();

        return aff.CalculateAffinity(other_ab.feature_list, this.feature_list, this.RR_radius);
    }
}
