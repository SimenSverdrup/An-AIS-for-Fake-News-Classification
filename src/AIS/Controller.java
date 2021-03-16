package AIS;

import Dataset.Dataset;
import Dataset.Parser;
import Features.FeatureExtractor;
import Features.Hasher;
import Features.Normaliser;
import GUI.GUI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static Dataset.Dataset.*;


public class Controller {
    public double total_accuracy;
    public double[] accuracies;
    public Antigen[] antigens; // all antigens
    public ArrayList<Antibody> antibodies;
    public ArrayList<Antibody> candidate_antibodies;

    public Antigen[][] antigens_split; // antigens split into k separate arrays, for k-fold cross-validation testing
    public ArrayList<Antigen> training_antigens; // antigens used for training (this iteration)
    public ArrayList<Antigen> testing_antigens; // antigens used for testing (this iteration)
    public ArrayList<Antibody> worst_performing_abs;

    public final int k = 3;   // k-fold cross validation split
    public final double antibody_ratio = 0.7;
    public final double max_antibody_replacement_ratio = 0.25;
    public final double feature_vector_mutation_probability = 1/((double) this.number_of_features);
    public final double RR_radius_mutation_probability = 1/((double) this.number_of_features);
    public final int generations = 300;
    public final double antibody_removal_threshold = 0.001; // the fitness value threshold for removing anitbodies

    public final Dataset dataset = SPIRALS; //FAKENEWSNET //FAKEDDIT //IRIS //SPIRALS //WINE // DIABETES (Pima Indian)
    public final int max_lines = 2000;
    public final int number_of_features = 2; // IRIS=4, SPIRALS=2, WINE=13, DIABETES=8

    private final boolean[] features_used = {true, false, false, false, false, false, false, false, false, false, false, false};
    // FEATURE_BAD_WORDS_TF, FEATURE_BAD_WORDS_TFIDF, FEATURE_NUMBER_OF_WORDS, FEATURE_POSITIVE_VS_NEGATIVE_WORDS,
    // FEATURE_NEGATION_WORDS_TF, FEATURE_EXCLUSIVE_WORDS_TF, FEATURE_SPECIAL_CHARACTERS, FEATURE_CAPITAL_LETTERS,
    // FEATURE_GRAMMAR, FEATURE_HEADLINE_WEIGHTING, FEATURE_PRECENCE_OF_NUMBERS, FEATURE_NLP = false;

    public void run() throws Exception {
        Parser parser = new Parser(this.dataset, this.max_lines + 1); // + 1 because first line are headers only

        List<List<String>> list = parser.getData();
        if (this.dataset.equals(FAKENEWSNET)) list.remove(0); // remove headers

        int number_of_records = list.size();
        this.antigens = new Antigen[number_of_records];
        this.training_antigens = new ArrayList<>();
        this.testing_antigens = new ArrayList<>();
        this.antibodies = new ArrayList<>();
        this.candidate_antibodies = new ArrayList<>();
        this.worst_performing_abs = new ArrayList<>();

        int i = 0;
        for (List<String> record : list) {
            this.antigens[i] = new Antigen(record, number_of_features, this.dataset);
            i++;
        }

        FeatureExtractor fe = new FeatureExtractor(features_used);
        Normaliser norm = new Normaliser();
        Affinity aff = new Affinity();

        // Shuffle the antigens
        this.antigens = shuffleAntigens(this.antigens);

        this.antigens_split = new Antigen[k][(int) Math.floor(this.antigens.length/(float) k)];

        for (int index=0; index<k; index++) {
            System.arraycopy(this.antigens, index*this.antigens_split[index].length, this.antigens_split[index], 0, this.antigens_split[index].length);
        }

        this.accuracies = new double[k];

        for (int k = 0; k < this.k; k++) {
            // k marks the index of the Antigen vector used for testing (this round)
            // note that each iteration 0->k-1 are independent, with feature values calculated from scratch each iteration

            // Copy the non-testing antigens to the training set
            this.training_antigens.clear();
            for (int idx=0; idx<this.k; idx++) {
                if (idx != k) {
                    this.training_antigens.addAll(Arrays.asList(this.antigens_split[idx]));
                }
            }

            // Extract features and initialise antibodies
            if ((this.dataset != IRIS) &&
                    (this.dataset != WINE) &&
                    (this.dataset != SPIRALS) &&
                    (this.dataset != DIABETES)) {
                this.training_antigens = fe.extractFeatures(this.training_antigens); // burde disse linjene byttes om?
            }

            this.training_antigens = norm.NormaliseFeatures(this.training_antigens);
            this.antibodies.clear();
            this.candidate_antibodies.clear();

            for (int j=0; j<this.training_antigens.size()*antibody_ratio; j++) {
                // Initialize antibodies using the random heuristic
                int rand = ThreadLocalRandom.current().nextInt(0, this.training_antigens.size() - 1);
                this.antibodies.add(new Antibody(this.training_antigens.get(rand)));
                //this.antibodies.get(this.antibodies.size()-1).random(this.training_antigens); // randomise feature values of added antibody
            }

            for (Antibody ab : this.antibodies) {
                ab.RR_radius = 100;

                /*
                Random rand = new Random();

                while (true) {
                    // Set antibody RR radius to euclidean distance to RANDOM ag of SAME class
                    int random_index = rand.nextInt(this.training_antigens.size());

                    if (this.training_antigens.get(random_index).true_class.equals(ab.true_class)) {
                        ab.RR_radius = aff.CalculateDistance(this.training_antigens.get(random_index).feature_list, ab.feature_list) + 0.001;
                        break;
                    }
                }
                */
                for (Antigen ag : antigens) {
                    if (!ag.true_class.equals(ab.true_class)) {
                        // Set antibody RR radius to euclidean distance to closest ag of DIFFERENT class (but not including the ag)
                        ab.RR_radius = Math.min(aff.CalculateDistance(ag.feature_list, ab.feature_list) - 0.001, ab.RR_radius);
                    }
                }
            }

            List<Antibody> clones = new ArrayList<>();
            List<Antibody> abs_to_be_deleted = new ArrayList<>();

            // Begin the training
            for (int generation=1; generation<=this.generations; generation++) {
                System.out.println("Generation " + generation + "/" + (this.generations));

                clones.clear();

                double reproduction_ratio = this.max_antibody_replacement_ratio * Math.pow(2/(double) this.antibodies.size(), (double) generation / ((double) (this.generations) * 1.5));
                int number_of_new_antibodies = (int) (reproduction_ratio * this.antibodies.size());

                if (number_of_new_antibodies < 1) {
                    System.out.println("New antibody rate too low. Breaking out of loop.");
                    break;
                }

                //System.out.println("Number of new antibodies this generation: " + number_of_new_antibodies);

                for (Antigen ag : this.training_antigens) {
                    // Need to find the affinity vector for each antigen, in order to accurately calculate the fitness of each ab
                    ag.findConnectedAntibodies(this.antibodies);
                }

                for (Antibody ab : this.antibodies) {
                    ab.findConnectedAntigens(this.training_antigens);
                    ab.calculateFitness(this.training_antigens);
                }

                HashMap<Integer, Double> fitnesses = new HashMap<>();

                int ab_index = 0;

                for (Antibody ab : this.antibodies) {
                    fitnesses.put(ab_index, ab.fitness);
                    ab_index++;
                }

                int antibody_clones_left = number_of_new_antibodies;


/*
                int number_of_classes = training_antigens.get(0).number_of_classes;
                String[] classes = training_antigens.get(0).classes;

                List<List<Antibody>> parents = new ArrayList<>();
                List<List<Antibody>> children = new ArrayList<>();


                for (int clss=0; clss<number_of_classes; clss++) {
                    List<Antibody> class_parents = new ArrayList<>();

                    for (Antibody ab : this.antibodies) {
                        if (classes[clss].equals(ab.true_class)) {
                            class_parents.add(ab);
                        }
                    }

                    parents.add(class_parents);
                }

                for (int clss=0; clss<number_of_classes; clss++) {

                    int children_per_class = number_of_new_antibodies/number_of_classes;

                    HashMap<Integer, Double> fitnesses1 = new HashMap<>();
                    int idx = 0;

                    for (Antibody ab : parents.get(clss)) {
                        fitnesses1.put(idx, ab.fitness);
                        idx++;
                    }

                    if (children_per_class % 2 != 0) children_per_class++;
                    List<Antibody> class_children = new ArrayList<>();

                    while (children_per_class > 0) {
                        int max_key = Collections.max(fitnesses1.entrySet(), Map.Entry.comparingByValue()).getKey();
                        fitnesses1.remove(max_key);

                        int max_key2 = Collections.max(fitnesses1.entrySet(), Map.Entry.comparingByValue()).getKey();
                        fitnesses1.remove(max_key2);

                        class_children.add(new Antibody(parents.get(clss).get(max_key)));

                        Random rand = new Random();

                        int split1 = rand.nextInt(this.number_of_features-1);
                        int split2 = Math.min(rand.nextInt(this.number_of_features-1) + split1, this.number_of_features-1);

                        double[] feature_vals1 = new double [split1];
                        double[] feature_vals2 = new double [split2 - split1];
                        double[] feature_vals3 = new double [this.number_of_features - split2];

                        for (int j=0; j<split1; j++) {
                            feature_vals1[j] = parents.get(clss).get(max_key).feature_list[j];
                        }
                        for (int j=split1; j<split2; j++) {
                            feature_vals2[j-split1] = parents.get(clss).get(max_key).feature_list[j];
                        }
                        for (int j=split2; j<this.number_of_features; j++) {
                            feature_vals2[j-split2] = parents.get(clss).get(max_key).feature_list[j];
                        }


                        int length = feature_vals1.length + feature_vals2.length + feature_vals3.length;
                        double[] all_feature_vals = new double[length];
                        int pos = 0;

                        for (double element : feature_vals1) {
                            all_feature_vals[pos] = element;
                            pos++;
                        }
                        for (double element : feature_vals2) {
                            all_feature_vals[pos] = element;
                            pos++;
                        }
                        for (double element : feature_vals3) {
                            all_feature_vals[pos] = element;
                            pos++;
                        }

                        class_children.get(class_children.size() - 1).feature_list = all_feature_vals;

                        children_per_class--;
                    }

                    for (Antibody child : class_children) {
                        child.mutate(1/(1 + (double) this.number_of_features), 1/(1 + (double) this.number_of_features));
                    }

                    children.add(class_children);
                }
*/

                while (antibody_clones_left > 0) {
                    int max_key = Collections.max(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(max_key);
                    clones.add(new Antibody(this.antibodies.get(max_key)));
                    antibody_clones_left--;
                }

                for (Antibody clone : clones) {
                    clone.reset();
                    clone.mutate(this.feature_vector_mutation_probability, this.RR_radius_mutation_probability);
                }

                fitnesses.clear();
                ab_index = 0;

                for (Antibody ab : antibodies) {
                    fitnesses.put(ab_index, ab.fitness);
                    ab_index++;
                }

                abs_to_be_deleted.clear();

                while (number_of_new_antibodies > 0) {
                    // Find antibodies in this.antibodies with poorest fitness
                    int min_key = Collections.min(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(min_key);
                    abs_to_be_deleted.add(this.antibodies.get(min_key));

                    number_of_new_antibodies--;
                }

                for (Antibody ab : abs_to_be_deleted) {
                    // Remove poorest performing antibodies from this.antibodies
                    this.antibodies.remove(ab);
                }

                // Add the new clones
                this.antibodies.addAll(clones);
            }


            for (int ab_idx=0; ab_idx<this.antibodies.size(); ab_idx++) {
                // remove abs with zero fitness
                this.antibodies.get(ab_idx).findConnectedAntigens(this.training_antigens);
                this.antibodies.get(ab_idx).calculateFitness(this.training_antigens);

                if (this.antibodies.get(ab_idx).fitness < this.antibody_removal_threshold) {
                    this.antibodies.remove(this.antibodies.get(ab_idx));
                    System.out.println("REMOVE");
                }
            }

            //-----------------Calculate accuracy, on training set------------------
            this.testing_antigens.clear();
            this.testing_antigens.addAll(Arrays.asList(this.antigens_split[k]));

            if ((this.dataset != IRIS) &&
                    (this.dataset != WINE) &&
                    (this.dataset != SPIRALS) &&
                    (this.dataset != DIABETES)) this.testing_antigens = fe.extractFeatures(this.testing_antigens);
            this.testing_antigens = norm.NormaliseFeatures(this.testing_antigens);

            double correct_predictions = 0;

            for (Antigen ag : this.testing_antigens) {
                ag.findConnectedAntibodies(this.antibodies);
                ag.predictClass(this.antibodies);

                System.out.println("Connected abs to this ag: " + ag.connected_antibodies.size());
                System.out.println("Ag feature list: " + Arrays.toString(ag.feature_list));
                System.out.println("Ag class: " + ag.true_class);
                System.out.println("Ag predicted class: " + ag.predicted_class);
                System.out.println("Ag class vote: " + Arrays.toString(ag.class_vote));
                System.out.println("\n");


                if (ag.true_class.equals(ag.predicted_class)) {
                    correct_predictions++;
                }
            }

            this.accuracies[k] = correct_predictions / this.testing_antigens.size();

            System.out.println("\n--------------------------------------");
            System.out.println("Accuracy for testing set k=" + k + ": " + this.accuracies[k]);
            System.out.println("--------------------------------------\n");


            /*
            ////////////////// DEBUG
            double distance = 0;
            int length = this.antibodies.get(4).feature_list.length;

            for (int ids=0; ids<length; ids++) {
                double diff = this.antibodies.get(4).feature_list[ids] - testing_antigens.get(4).feature_list[ids];
                distance += Math.pow(diff, 2);
            }

            System.out.println("Random ab-ag distance: " + Math.sqrt(distance));
            //////////////////
            */
        }

        //----------Calculate total accuracy------------
        double total_acc = 0;

        for (double acc : this.accuracies) {
            total_acc += acc;
        }
        this.total_accuracy = total_acc/this.accuracies.length;
        System.out.println("\nTotal accuracy: " + total_acc/this.accuracies.length);


        // Algorithm:
        // Extract features
        // Normalise features (AIRS claims that the exact normalisation function doesn't matter, but within [0,1])
        // Put features into feature vectors
        // Select feature vectors to be antibodies and antigens (select n at random)
        // Add RR radius field to antibodies (initialised to some value, can use different heuristics)
            // Start with big radius
            // Initialiser RR radius til nærmeste AG med annen klasse (slik som i AISLFS) eller kun for å omfatte nærmeste AG uansett klasse (slik som i MAIM)
        // Split antigens into test and training (with k-fold cross-validation testing (with k=10?))
        // Train antibodies:
            // For each antibody:
            // While more antigens available for training:
                // Expose AIS to antigen (as feature vector, not raw)
                // For every antibody:
                    // Iterate through all the antigens in the training set (remember, cross-validation)
                    // See if antigen is within RR
                    // If it is:
                        // Check if antigen has the same class
                        // Calculate affinity to antigen (Manhattan/Eucledian)
                        // Mutate antibody with probability inversely proportional to affinity (AIRS):
                            // If antigen and antibody has different class, increase the mutation probability
                            // Clone antibody to several clones
                            // Randomly mutate the clone:
                                // Every feature value can potentially be mutated, by some probability
                                // Remember to still keep the mutation rate fairly low, as this is done for every antigen
                                // Mutation can be the multiplication with a number between 0.1 and 2, like MAIM
                                    // This also includes the RR radius
                                    // Perhaps it's smarter to add/subtract some random number instead?
                                // Can add some heuristic where the feature value closest to the antigen has a greater probability of mutation
                                    // This must take into account whether the antigen class is equal or not
                            // Replace the parent antibody with the best-performing clone, like in AISLFS
                                // Use the fitness evaluation from VALIS and MAIM
                                // F(b) = SharingFactor*WeightedAccuracy/AG_interactions
                                // (If none of the clones connect to an antigen, generate new ones?)
                    // If not:
                        // Jump to next antigen
                        // Is this smart? Antibodies
        // (Perform apothesis of unneeded antibodies)
            // Replace the n antibodies which didn't detect a single antigen with the n least detected antigens?
            // Sort of like in AISLFS
            // Can also remove antibodies with affinity < some value, with each other
        // Test AIS on the testing set antigens
            // For each antigen in testing set:
                // Calculate predicted class of antigen according to some voting heuristic
                    // Voting heuristic based on the one from AISLFS where the affinities are considered
                // Calculate (+plot) accuracy
    }

    public Antigen[] shuffleAntigens(Antigen[] array) {
        // Implementing Fisher–Yates shuffle, from https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array

        Random rnd = ThreadLocalRandom.current();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            Antigen temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
        return array;
    }

    public static boolean contains(final int[] arr, final int key) {
        return Arrays.stream(arr).anyMatch(i -> i == key);
    }

    public int getIndexOfMin(ArrayList<Antibody> data) {
        double min = Double.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < data.size(); i++) {
            double f = data.get(i).fitness;
            if (Double.compare(f, min) < 0) {
                min = f;
                index = i;
            }
        }
        return index;
    }

    public void TestParser(List<List<String>> list) {
        // Test the dataset parsing

        System.out.print(list.get(0) + "\n");
        System.out.print(list.get(40) + "\n");
    }

    public void TestAffinity() {
        // Test Affinity

        double[] feature_vector1 = {0.35, 0.35, 0.68};
        double[] feature_vector2 = {0.22, 0.30, 0.70};

        Affinity aff = new Affinity();
        double num = aff.CalculateAffinity(feature_vector1, feature_vector2, 100);

        System.out.println(num);
    }

    public void TestNormaliser() {
        // Test Normaliser


    }

    public void TestMutate() {
        // Test Mutate
        double[] vector = {0.35, 0.35, 0.68, 0.44, 0.33};
        System.out.println("Old list: " + Arrays.toString(vector));

        Mutate mutate = new Mutate();
        double[] vector2 = mutate.mutateVector(vector, 0.6);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector, 0.6);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector, 0.6);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector, 0.6);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector, 0.6);
        System.out.println("New list: " + Arrays.toString(vector2));

        System.out.println("Old value: " + 0.5);
        double val = mutate.mutateScalar(0.5, 0.6);
        System.out.println("New value: " + val);
    }

    public void TestHasher() {
        // Test the hashing class

        Hasher hasher = new Hasher();

        double hash = hasher.StringToHash("politifact.com");
        System.out.println("Hash: " + hash);

        hash = hasher.StringToHash("wiseye.org");
        System.out.println("Hash: " + hash);

        hash = hasher.StringToHash("twitter.com");
        System.out.println("Hash: " + hash);

        hash = hasher.StringToHash("politifact.com");
        System.out.println("Hash: " + hash);
    }
}
