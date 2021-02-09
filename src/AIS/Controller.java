package AIS;

import Dataset.Dataset;
import Dataset.Parser;
import Features.FeatureExtractor;
import Features.Hasher;
import Features.Normaliser;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static Dataset.Dataset.FAKENEWSNET;

public class Controller {
    private final int max_lines = 1000;
    private final int k = 10;   // k-fold cross validation split
    private final int number_of_antibody_clones = 15; // the amount of clones for each antibody
    private final double RR_radius = 0.1;
    private final Dataset dataset = FAKENEWSNET;
    private final double antibody_ratio = 0.08;
    private final int number_of_features = 1;
    private final int antibody_training_cycles = 50;
    private final boolean[] features_used = {true, false, false, false, false, false, false, false, false, false, false, false};
    // FEATURE_BAD_WORDS_TF, FEATURE_BAD_WORDS_TFIDF, FEATURE_NUMBER_OF_WORDS, FEATURE_POSITIVE_VS_NEGATIVE_WORDS,
    // FEATURE_NEGATION_WORDS_TF, FEATURE_EXCLUSIVE_WORDS_TF, FEATURE_SPECIAL_CHARACTERS, FEATURE_CAPITAL_LETTERS,
    // FEATURE_GRAMMAR, FEATURE_HEADLINE_WEIGHTING, FEATURE_PRECENCE_OF_NUMBERS, FEATURE_NLP = false;



    public Controller() throws FileNotFoundException, URISyntaxException {
        Parser parser = new Parser(this.dataset, this.max_lines + 1); // + 1 because first line are headers only

        List<List<String>> list = parser.getData();
        list.remove(0); // remove headers

        int number_of_records = list.size();
        Antigen[] antigens = new Antigen[number_of_records];

        int i = 0;
        for (List<String> record : list) {
            antigens[i] = new Antigen(record, number_of_features);
            i++;
        }

        FeatureExtractor fe = new FeatureExtractor(features_used);
        antigens = fe.extractFeatures(antigens);

        Normaliser norm = new Normaliser();
        antigens = norm.NormaliseFeatures(antigens);

        Antibody[] antibodies = new Antibody[(int) Math.floor(antigens.length*antibody_ratio)];

        for (int j=0; j<antibodies.length; j++) {
            int rand = ThreadLocalRandom.current().nextInt(0, antigens.length-1);
            antibodies[j] = new Antibody(antigens[rand]);
            antibodies[j].RR_radius = this.RR_radius;
        }

        Antigen[][] antigens_split = new Antigen[k][(int) Math.floor(antigens.length/(float) k)];

        for (int index=0; index<k; index++) {
            System.arraycopy(antigens, index*antigens_split[index].length, antigens_split[index], 0, antigens_split[index].length);
        }

        double[] accuracies = new double[k];

        for (int k = 0; k<this.k; k++) {
            // k marks the index of the Antigen vector used for testing (this round)

            for (int antibody_index=0; antibody_index<antibodies.length; antibody_index++) {
                double best_score = 0;

                for (Antigen[] antigen_vector : antigens_split) {
                    if (antigen_vector == antigens_split[k]) continue; // we don't want to use this vector for training this round, only testing
                    boolean new_round = true; // the antibody will repeat the cloning and mutation process if a clone outperformed the parent in the previous iteration

                    while (new_round) {
                        new_round = false;

                        for (Antigen antigen : antigen_vector) {
                            // First we need to find the current connected antibodies to every antigen,
                            // as this information is used in the antibody fitness calculation
                            antigen.findConnectedAntibodies(antibodies);
                        }

                        antibodies[antibody_index].calculateFitness(antigen_vector);

                        Antibody[] antibody_clones = new Antibody[number_of_antibody_clones]; // the clones of the specific, single antibody

                        for (int index=0; index<number_of_antibody_clones; index++) {
                            // Generate, mutate and calculate fitness of the antibody clones,
                            // the best performing one replaces the parent, if the fitness value is better
                            // (could also do as in AISLFS and have the best-performing clone replace the parent regardless)
                            antibody_clones[index] = new Antibody(antibodies[antibody_index]);
                        }

                        int clone_number = 0;
                        int best_clone_index = (int) (Math.random() * (number_of_antibody_clones - 1));
                        best_score = antibodies[antibody_index].fitness; // initialised to the parent's fitness

                        for (Antibody clone : antibody_clones) {
                            // fitness value is saved to member variable, note: we don't need to call findConnectedAntigens (as this is called within calculateFitness)
                            clone.mutate();
                            clone.calculateFitness(antigen_vector);

                            if (clone.fitness > best_score) {
                                best_score = clone.fitness;
                                best_clone_index = clone_number;
                                new_round = true;
                            }

                            clone_number++;
                        }

                        if (best_score > antibodies[antibody_index].fitness) antibodies[antibody_index] = new Antibody(antibody_clones[best_clone_index]); // NOTE, may lead to local optima
                    }
                }
            }

            double correct_predictions = 0;

            for (Antigen ag : antigens_split[k]) {
                ag.findConnectedAntibodies(antibodies);
                System.out.println("\nNumber of antibodies connected to this antigen: " + ag.connected_antibodies.size());

                ag.predictClass();
                if (ag.true_class.equals(ag.predicted_class)) {
                    correct_predictions++;
                }
            }

            accuracies[k] = correct_predictions/antigens_split[k].length;

            System.out.println("\n--------------------------------------");
            System.out.println("Accuracy for testing set k=" + k + ": " + accuracies[k]);
            System.out.println("--------------------------------------\n");
        }

        double total_acc = 0;

        for (double acc : accuracies) {
            total_acc += acc;
        }
        System.out.println("\nTotal accuracy: " + total_acc/accuracies.length);


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
        // Test AIS on the testing set antigens
            // For each antigen in testing set:
                // Calculate predicted class of antigen according to some voting heuristic
                    // Voting heuristic based on the one from AISLFS where the affinities are considered
                // Calculate (+plot) accuracy
    }

    public static boolean contains(final int[] arr, final int key) {
        return Arrays.stream(arr).anyMatch(i -> i == key);
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
        double[] vector2 = mutate.mutateVector(vector);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector);
        System.out.println("New list: " + Arrays.toString(vector2));

        vector2 = mutate.mutateVector(vector);
        System.out.println("New list: " + Arrays.toString(vector2));

        System.out.println("Old value: " + 0.5);
        double val = mutate.mutateScalar(0.5);
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
