package AIS;

import Dataset.Dataset;
import Dataset.Parser;
import Features.FeatureExtractor;
import Features.Hasher;
import Features.Normaliser;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static Dataset.Dataset.FAKENEWSNET;

public class Controller {
    public double total_accuracy;
    public double[] accuracies;
    public Antigen[] antigens;
    public ArrayList<Antibody> antibodies;
    public Antigen[][] antigens_split;
    public ArrayList<Antigen> training_antigens;
    public ArrayList<Antigen> testing_antigens;
    public ArrayList<Antibody> worst_performing_abs;
    private boolean run;
    private final int antibody_clones_to_keep = 5;  // the number of antibody clones to keep, best performing with regards to affinity
    private final int max_lines = 1000;
    private final int k = 3;   // k-fold cross validation split
    private final int number_of_antibody_clones = 6; // the amount of clones for each antibody
    private final double initial_RR_radius = 0.1;
    private final Dataset dataset = FAKENEWSNET;
    private final double antibody_ratio = 0.1;
    private final int number_of_features = 1;
    private final int iterations = 10;
    private final int cycles = 30;
    private final int allowed_iterations_without_improvement = 3;
    private final double antibody_removal_ratio = 0.1; // the percentage of the antibodies we replace each round, based on fitness
    public final double testing_ratio = 0.25;
    private final double antibody_affinity_threshold = 0.975; // the threshold for how high the affinity between two antibodies should be before one of them is deleted
    private final double antibody_deletion_ratio = 0.08; // the ratio of antibodies to be deleted each iteration, with poorest fitness
    private final boolean[] features_used = {true, false, false, false, false, false, false, false, false, false, false, false};
    // FEATURE_BAD_WORDS_TF, FEATURE_BAD_WORDS_TFIDF, FEATURE_NUMBER_OF_WORDS, FEATURE_POSITIVE_VS_NEGATIVE_WORDS,
    // FEATURE_NEGATION_WORDS_TF, FEATURE_EXCLUSIVE_WORDS_TF, FEATURE_SPECIAL_CHARACTERS, FEATURE_CAPITAL_LETTERS,
    // FEATURE_GRAMMAR, FEATURE_HEADLINE_WEIGHTING, FEATURE_PRECENCE_OF_NUMBERS, FEATURE_NLP = false;

    public void run() throws FileNotFoundException, URISyntaxException {
        this.run = true;
        Parser parser = new Parser(this.dataset, this.max_lines + 1); // + 1 because first line are headers only

        List<List<String>> list = parser.getData();
        list.remove(0); // remove headers

        int number_of_records = list.size();
        this.antigens = new Antigen[number_of_records];
        this.training_antigens = new ArrayList<>();
        this.testing_antigens = new ArrayList<>();
        this.antibodies = new ArrayList<>();
        this.worst_performing_abs = new ArrayList<>();

        int i = 0;
        for (List<String> record : list) {
            this.antigens[i] = new Antigen(record, number_of_features);
            i++;
        }

        FeatureExtractor fe = new FeatureExtractor(features_used);
        Normaliser norm = new Normaliser();
        this.antigens = norm.NormaliseFeatures(this.antigens);

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
            this.training_antigens = fe.extractFeatures(this.training_antigens);
            this.antibodies.clear();

            for (int j=0; j<this.training_antigens.size()*antibody_ratio; j++) {
                int rand = ThreadLocalRandom.current().nextInt(0, this.training_antigens.size() - 1);
                this.antibodies.add(new Antibody(this.training_antigens.get(rand)));
                this.antibodies.get(j).RR_radius = this.initial_RR_radius;
            }

            // We now begin the training
            for (int cycle=0; cycle<this.cycles; cycle++) {
                System.out.println("Starting cycle " + cycle + " out of " + (this.cycles - 1));

                for (int antibody_index = 0; antibody_index < this.antibodies.size(); antibody_index++) {
                    int rounds_without_improvement = 0;

                    for (int iteration = 0; iteration < this.iterations; iteration++) {
                        double best_score = 0;

                        for (Antigen antigen : this.training_antigens) {
                            // First we need to find the current connected antibodies to every antigen,
                            // as this information is used in the antibody fitness calculation
                            antigen.findConnectedAntibodies(this.antibodies);
                        }

                        this.antibodies.get(antibody_index).calculateFitness(this.training_antigens);

                        Antibody[] antibody_clones = new Antibody[number_of_antibody_clones]; // the clones of the specific, single antibody

                        for (int index = 0; index < number_of_antibody_clones; index++) {
                            // Generate, mutate and calculate fitness of the antibody clones,
                            // the best performing one replaces the parent, if the fitness value is better
                            // (could also do as in AISLFS and have the best-performing clone replace the parent regardless)
                            antibody_clones[index] = new Antibody(this.antibodies.get(antibody_index));
                        }

                        int clone_number = 0;
                        int best_clone_index = (int) (Math.random() * (number_of_antibody_clones - 1));
                        best_score = this.antibodies.get(antibody_index).fitness; // initialised to the parent's fitness

                        double[] antibody_fitnesses = new double[this.antibodies.size()];
                        int idx = 0;
                        for (Antibody ab : this.antibodies) {
                            antibody_fitnesses[idx] = ab.fitness;
                            idx++;
                        }

                        Arrays.sort(antibody_fitnesses);
                        double mutation_prob = 0.7; // initialised

                        for (int index = 0; index < antibody_fitnesses.length; index++) {
                            if (this.antibodies.get(antibody_index).fitness == antibody_fitnesses[index]) {
                                // We find the fitness of the current antibody, compared to the other antibodies
                                // Mutate clones inversely proportional to fitness of parent (compared to the other antibodies)

                                mutation_prob = 1 - (double) index / (antibody_fitnesses.length - 1);

                                break;
                            }
                        }

                        for (Antibody clone : antibody_clones) {
                            // fitness value is saved to member variable, note: we don't need to call findConnectedAntigens (as this is called within calculateFitness)
                            clone.mutate(mutation_prob, mutation_prob * 0.7);
                            clone.calculateFitness(this.training_antigens);

                            if (clone.fitness >= best_score) {
                                // equal or greater fitness (not just greater) to avoid local optima, unable to escape
                                best_score = clone.fitness;
                                best_clone_index = clone_number;
                            }

                            clone_number++;
                        }

                        if (best_score >= this.antibodies.get(antibody_index).fitness) {
                            // Replace the parent
                            this.antibodies.remove(this.antibodies.get(antibody_index));
                            this.antibodies.add(new Antibody(antibody_clones[best_clone_index])); // NOTE, may lead to local optima
                            if (best_score > this.antibodies.get(antibody_index).fitness)
                                rounds_without_improvement = 0;
                            else {
                                rounds_without_improvement++;
                            }
                        } else {
                            // no improvement among clones, this round
                            rounds_without_improvement++;
                        }

                        if (rounds_without_improvement > this.allowed_iterations_without_improvement) {
                            //System.out.println("No improvement in " + rounds_without_improvement + " rounds. Skipping to next antibody.");
                            break;
                        }
                    }
                }

                worst_performing_abs.clear();

                for (int rmv = 0; rmv < Math.floor(this.antibody_removal_ratio * this.antibodies.size()); rmv++) {
                    worst_performing_abs.add(this.antibodies.get(rmv));
                }

                for (Antibody ab : this.antibodies) {
                    int worst_idx = this.getIndexOfMin(worst_performing_abs);
                    double worst_fitness = worst_performing_abs.get(worst_idx).fitness;

                    if (ab.fitness < worst_fitness) {
                        worst_performing_abs.remove(worst_idx);
                        worst_performing_abs.add(ab);
                    }
                }

                this.antibodies.removeAll(worst_performing_abs);

                for (int new_ab_idx = 0; new_ab_idx < worst_performing_abs.size(); new_ab_idx++) {
                    Antibody new_ab = new Antibody(this.antibodies.get((int) (Math.random() * this.antibodies.size())));
                    new_ab.random();
                    this.antibodies.add(new_ab);
                }
            }

            //-----------------Calculate accuracy, on training set------------------
            this.testing_antigens.clear();
            this.testing_antigens.addAll(Arrays.asList(this.antigens_split[k]));
            this.testing_antigens = fe.extractFeatures(this.testing_antigens);

            double correct_predictions = 0;

            for (Antigen ag : this.testing_antigens) {
                ag.findConnectedAntibodies(this.antibodies);
                //System.out.println("\nNumber of antibodies connected to this antigen: " + ag.connected_antibodies.size());

                ag.predictClass();
                if (ag.true_class.equals(ag.predicted_class)) {
                    correct_predictions++;
                }
            }

            this.accuracies[k] = correct_predictions / this.testing_antigens.size();

            System.out.println("\n--------------------------------------");
            System.out.println("Accuracy for testing set k=" + k + ": " + this.accuracies[k]);
            System.out.println("--------------------------------------\n");
        }


        //----------Calculate total accuracy------------
        double total_acc = 0;

        for (double acc : this.accuracies) {
            total_acc += acc;
        }
        this.total_accuracy = total_acc/this.accuracies.length;
        System.out.println("\nTotal accuracy: " + total_acc/this.accuracies.length);



        // TODO: Implementer en GUI for å se accuracy over tid og gjerne plott antibodies med RR og antigens i 2D
        // TODO: vurder om du burde slette antigensa som også har blitt til antibodies, fra antigens
        // TODO: lag noe bedre heuristikk for RR radius initialisering og vurder om denne burde mutere med høyere sannsynlighet


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

    public void stopRunning() {
        this.run = false;
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
