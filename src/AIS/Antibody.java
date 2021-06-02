package AIS;


import Dataset.Dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Antibody {
    public String true_class;
    public String id;
    public int number_of_classes;   // number of classes
    public double RR_radius;
    public double[] feature_list; // note, length not equal to raw_list - as these are the floating feature values
    public boolean[] features_used; // the features used (local feature selection)
    public List<Antigen> connected_antigens; // the antigens which this antibody is connected to (within RR)
    public List<Double> affinities; // the affinities to the antigens (must be in the same order as antigens)
    public double fitness;
    public double correct_AG_interactions;
    public double weighted_accuracy;
    public double single_aff; // the affinity to a single antigen
    public Dataset dataset;
    public int parent_index;

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antibody(Antigen antigen) {
        // Constructor taking an antigen input
        this.dataset = antigen.dataset;
        this.feature_list = antigen.feature_list.clone();
        this.true_class = antigen.true_class;

        Random rand = new Random();
        this.id = String.valueOf((int) Math.floor(rand.nextInt(1000000)));

        this.connected_antigens = new ArrayList<>();
        this.affinities = new ArrayList<>();
        this.number_of_classes = antigen.number_of_classes;

        this.features_used = new boolean[this.feature_list.length];
        for (int i=0; i<this.feature_list.length; i++) {
            features_used[i] = true;
        }
    }

    public Antibody(Antibody antibody) {
        // Constructor taking another antibody as input
        this.dataset = antibody.dataset;
        this.true_class = antibody.true_class;
        this.id = antibody.id;

        this.feature_list = antibody.feature_list.clone();
        this.features_used = antibody.features_used.clone();
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
        this.correct_AG_interactions = 0;
    }

    public void findConnectedAntigens(ArrayList<Antigen> antigens) {
        // Antigens input argument is all the antigens

        this.reset();

        Affinity aff = new Affinity();

        this.connected_antigens = new ArrayList<>();
        this.affinities = new ArrayList<>();

        for (Antigen ag : antigens) {
            double affinity = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius, this.features_used);
            if (affinity > 0) {
                // The antigen is within the RR
                this.affinities.add(affinity);
                this.connected_antigens.add(ag);
            }
        }
    }

    public void calculateFitness(ArrayList<Antigen> antigens) {
        // Antigens input argument is all the antigens
        // Fitness function from MAIM and VALIS
        // F(b) = SharingFactor*WeightedAccuracy/AG_interactions

        this.findConnectedAntigens(antigens); // we fill up this.affinities and this.connected_antigens here
        int number_of_connected_antigens = this.connected_antigens.size();

        if (number_of_connected_antigens == 0) {
            this.fitness = 0.0;
        }
        else {
            double sharing_factor = 0;
            int ag_idx = 0;
            double ag_affinities = 0;
            double total_affinities = 0;
            double AB_interactions = 0;

            for (double affinity : this.affinities) {
                // Sum up all the affinities to connected antigens
                total_affinities += affinity;
            }

            for (Antigen antigen : this.connected_antigens) {

                // Sum up all the affinities to connected antigens with the same class
                if (antigen.true_class.equals(this.true_class)) {
                    this.correct_AG_interactions += this.affinities.get(ag_idx);
                }

                // Calculate the interaction share and add to the sharing factor
                ag_affinities = 0;
                for (double ag_aff : antigen.affinities) {
                    // Note, the antigen's affinities (not this antibody's affinities)
                    ag_affinities += ag_aff;
                }

                AB_interactions = this.affinities.get(ag_idx);
                sharing_factor += Math.pow(AB_interactions, 2)/(ag_affinities); //part of the antigen that belongs to the antibody

                ag_idx++;
            }

            this.weighted_accuracy = (1.5 + this.correct_AG_interactions)/(this.number_of_classes + total_affinities);  // Apply Laplacian smoothing, from VALIS (or not) - ser ut som du får bedre resultater uten

            this.fitness = ((sharing_factor*weighted_accuracy)/(total_affinities));
        }
    }

    public void random(List<Antigen> antigens) {
        // Initialises the antibody randomly, with RR radius set to euclidean distance to random ag of same class
        // and feature values are set randomly within a span of +-10% of corresponding max/min value in antigens of same class
        // NOTE: don't need to set class, this is taken from antigen/antibody parent

        this.reset();
        Affinity aff = new Affinity();
        Random r = new Random();

        for (int idx=0; idx<this.feature_list.length; idx++) {
            // set feature values randomly, within +-10% of max/min corresponding values in same-class antigens

            double max_val = 0; // maximum value at index idx, in antigens
            double min_val = 1000; // minimum value at index idx, in antigens
            for (Antigen ag : antigens) {
                if (ag.true_class.equals(this.true_class)) {
                    if (ag.feature_list[idx] > max_val) max_val = ag.feature_list[idx];
                    else if (ag.feature_list[idx] < min_val) min_val = ag.feature_list[idx];
                }
                else {
                    // Set antibody RR radius to euclidean distance to closest ag of DIFFERENT class (but not including)
                    this.RR_radius = Math.min(aff.CalculateDistance(ag.feature_list, this.feature_list, this.features_used) - 0.001, this.RR_radius);
                }
            }
            max_val = max_val*1.1;
            min_val = min_val*0.9;

            this.feature_list[idx] = min_val + (max_val - min_val) * r.nextDouble();
        }

        this.id = String.valueOf((int) (Math.random()*10000));
    }

    public void setNewID() {
        Random rand = new Random();
        this.id = String.valueOf((int) Math.floor(rand.nextInt(1000000)));
    }

    public void mutate(double vector_mutation_prob, double scalar_mutation_prob) {
        Mutate mut = new Mutate();
        float features_used_probability = 1/ (float) (1+this.feature_list.length);

        double previous_RR = this.RR_radius;
        double[] previous_feature_list = this.feature_list.clone();

        // randomly mutate which features to use (mutation is not forced, like for RR radius and feature values)
        this.features_used = mut.mutateFeatureSubset(features_used, features_used_probability);

        do {
            this.RR_radius = mut.mutateScalar(this.RR_radius, scalar_mutation_prob);
            this.feature_list = mut.mutateVector(this.feature_list, vector_mutation_prob, this.features_used);
        } while ((this.RR_radius == previous_RR) && Arrays.equals(this.feature_list, previous_feature_list));
    }

    public void setParentIndex(int index) {
        this.parent_index = index;
    }

    public boolean equals(Antibody other_ab) {
        return this.id.equals(other_ab.id);
    }

    public void calculateAffinity(Antigen ag) {
        Affinity aff = new Affinity();

        this.single_aff = aff.CalculateAffinity(ag.feature_list, this.feature_list, this.RR_radius, this.features_used);
    }

    public double calculateAffinity(Antibody other_ab) {
        Affinity aff = new Affinity();

        return aff.CalculateAffinity(other_ab.feature_list, this.feature_list, this.RR_radius, this.features_used);
    }
}
