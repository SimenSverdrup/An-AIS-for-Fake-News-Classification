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

    private final int k = 3;   // k-fold cross validation split
    private final double antibody_ratio = 0.6;
    private final int generations = 300;

    private final Dataset dataset = IRIS; //FAKENEWSNET //FAKEDDIT //IRIS //SPIRALS //WINE // DIABETES (Pima Indian)
    private final int max_lines = 2000;
    private final int number_of_features = 4; // IRIS=4, SPIRALS=2, WINE=13, DIABETES=8

    private final boolean[] features_used = {true, false, false, false, false, false, false, false, false, false, false, false};
    // FEATURE_BAD_WORDS_TF, FEATURE_BAD_WORDS_TFIDF, FEATURE_NUMBER_OF_WORDS, FEATURE_POSITIVE_VS_NEGATIVE_WORDS,
    // FEATURE_NEGATION_WORDS_TF, FEATURE_EXCLUSIVE_WORDS_TF, FEATURE_SPECIAL_CHARACTERS, FEATURE_CAPITAL_LETTERS,
    // FEATURE_GRAMMAR, FEATURE_HEADLINE_WEIGHTING, FEATURE_PRECENCE_OF_NUMBERS, FEATURE_NLP = false;

    public void run() throws FileNotFoundException, URISyntaxException {
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

                double reproduction_ratio = 0.25 * Math.pow(2/(double) this.antibodies.size(), (double) generation / ((double) (this.generations) * 1.5));
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

                while (antibody_clones_left > 0) {
                    int max_key = Collections.max(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(max_key);
                    clones.add(new Antibody(this.antibodies.get(max_key)));
                    antibody_clones_left--;
                }

                for (Antibody clone : clones) {
                    //System.out.println("\nAntibody RR radius: " + clone.RR_radius);
                    //System.out.println("Antibody feature list: " + Arrays.toString(clone.feature_list));
                    clone.reset();
                    clone.mutate(1/(1 + (double) this.number_of_features), 1/(1 + (double) this.number_of_features));
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



                /*
                this.candidate_antibodies.clear();

                int rand = ThreadLocalRandom.current().nextInt(0, this.training_antigens.size() - 1);
                Antigen random_antigen = this.training_antigens.get(rand); // get random antigen used for training this generation

                HashMap<Integer, Double> affinities = new HashMap<>();
                ArrayList<Antibody> clones = new ArrayList<>();

                int antibody_index = 0;

                for (Antibody memory_ab : this.antibodies) {
                    if (memory_ab.true_class.equals(random_antigen.true_class)) {

                        affinities.put(antibody_index, 1/aff.CalculateDistance(random_antigen.feature_list, memory_ab.feature_list));
                        // burde jeg sjekke om innenfor RR radius?
                        // affinity blir null her, om utenfor RR
                    }
                    antibody_index++;
                }

                int number_of_antibodies_to_reproduce = (int) Math.min(this.antibody_reproduction_ratio*this.antibodies.size(), affinities.size());
                int rank = 1; // the rank of the antibody (according to affinity to antigen)
                double mutation_prob; // will be calculated according to rank

                while (number_of_antibodies_to_reproduce > 0) {
                    int max_index = Collections.max(affinities.entrySet(), Map.Entry.comparingByValue()).getKey();
                    int number_of_clones = (int) Math.ceil((this.number_of_antibody_clones/(double) rank)); // from CLONALG, but somewhat adapted

                    while (number_of_clones > 0) {
                        Antibody clone = new Antibody(this.antibodies.get(max_index));
                        clone.reset();
                        mutation_prob = Math.min(0.7, (1/((double) (this.number_of_features))) + ((double) (rank) - 1)/affinities.size());
                        clone.mutate(mutation_prob, mutation_prob);
                        clone.setParentIndex(max_index);
                        clones.add(clone);

                        number_of_clones--;
                    }

                    affinities.remove(max_index);
                    number_of_antibodies_to_reproduce--;
                    rank++;
                }

                affinities.clear();
                int clone_index = 0;
                for (Antibody clone : clones) {
                    affinities.put(clone_index, 1/aff.CalculateDistance(random_antigen.feature_list, clone.feature_list));
                    clone_index++;
                }

                int remaining_clones = Math.min((int) (this.antibody_replacement_ratio*this.antibodies.size()), clones.size());

                while (remaining_clones > 0) {
                    // Add the best performing clones to the candidate pool
                    int max_index = Collections.max(affinities.entrySet(), Map.Entry.comparingByValue()).getKey();
                    this.candidate_antibodies.add(clones.get(max_index));
                    affinities.remove(max_index);
                    remaining_clones--;
                }

                affinities.clear();
                clone_index = 0;
                for (Antibody clone : this.candidate_antibodies) {
                    int matches = 0;
                    double cumulative_affinities = 0;
                    for (Antigen ag : this.training_antigens) {
                        if (clone.true_class.equals(ag.true_class)) {
                            double affinity = 1/aff.CalculateDistance(ag.feature_list, clone.feature_list);
                            if (affinity > 0) {
                                // if inside RR and same class
                                cumulative_affinities += affinity;
                                matches++;
                            }
                        }
                    }
                    affinities.put(clone_index, cumulative_affinities/(double) (matches)); // this is the average affinity to same-class antigens within the RR of the clone
                    clone_index++;
                }

                List<Antibody> clones_to_be_removed = new ArrayList<>();
                clone_index = 0;
                int[] antigen_indices = new int[this.candidate_antibodies.size()];

                for (Antibody clone : this.candidate_antibodies) {
                    int matches = 0;
                    int antigen_index = 0;
                    for (Antigen ag : this.training_antigens) {
                        if ((1/aff.CalculateDistance(ag.feature_list, clone.feature_list) > affinities.get(clone_index))
                                && (!clone.true_class.equals(ag.true_class))) {
                            // if different class and higher affinity than average affinity to same-class antigens
                            System.out.println("Other-class antigen closer than average distance to same-class!");
                            matches++;
                            if (matches > 1) break;
                            antigen_indices[clone_index] = antigen_index;
                        }
                        antigen_index++;
                    }
                    if (matches > 1) clones_to_be_removed.add(this.candidate_antibodies.get(clone_index));
                    else if (matches == 1) this.training_antigens.remove(antigen_indices[clone_index]); // consider the antigen as noise/outlier and delete from training set
                    clone_index++;
                }

                for (Antibody clone : clones_to_be_removed) {
                    this.candidate_antibodies.remove(clone);
                }

                for (Antibody candidate : this.candidate_antibodies) {
                    Antibody parent = this.antibodies.get(candidate.parent_index);

                    if (1/aff.CalculateDistance(random_antigen.feature_list, parent.feature_list) < 1/aff.CalculateDistance(random_antigen.feature_list, candidate.feature_list)) {
                        // replace the parent
                        this.antibodies.remove(candidate.parent_index);
                        this.antibodies.add(candidate);
                    }
                }

                HashMap<String, Integer> class_distribution = new HashMap<>();
                String[] keys = new String[this.number_of_features];
                int key_idx = 0;

                for (Antibody ab : this.antibodies) {
                    if (class_distribution.containsKey(ab.true_class)) {
                        int temp = class_distribution.get(ab.true_class);
                        class_distribution.remove(ab.true_class);
                        class_distribution.put(ab.true_class, temp+1);
                    }
                    else {
                        class_distribution.put(ab.true_class, 1);
                        keys[key_idx] = ab.true_class;
                        key_idx++;
                    }
                }

                if (Math.random() > 0.3) {
                    // Replace the poorest-performing antibody (in terms of affinity to the random ag) with a new random one

                    HashMap<Integer, Double> memory_ab_affinities = new HashMap<>();
                    int idx = 0;

                    String min_class = class_distribution.entrySet().stream().filter(p -> Arrays.asList(keys).contains(p.getKey())).min(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();

                    for (Antibody ab : this.antibodies) {
                        if (ab.true_class.equals(random_antigen.true_class)) {
                            memory_ab_affinities.put(idx, 1/aff.CalculateDistance(random_antigen.feature_list, ab.feature_list));
                        }
                        idx++;
                    }

                    int min_index = Collections.min(memory_ab_affinities.entrySet(), Map.Entry.comparingByValue()).getKey();
                    if (!this.antibodies.get(min_index).true_class.equals(min_class)) {
                        this.antibodies.remove(min_index);
                        rand = ThreadLocalRandom.current().nextInt(0, this.training_antigens.size() - 1);
                        this.antibodies.add(new Antibody(this.training_antigens.get(rand)));
                        this.antibodies.get(this.antibodies.size()-1).random(this.training_antigens);
                    }
                }
                */


                /*
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
                 */



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
