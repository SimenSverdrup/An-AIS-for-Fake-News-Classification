package AIS;

import Dataset.Dataset;
import Dataset.Parser;
import Features.FeatureExtractor;
import Features.Hasher;
import Features.Normaliser;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static Dataset.Dataset.FAKENEWSNET;

public class Controller {
    private final int max_lines = 30;
    private final Dataset dataset = FAKENEWSNET;
    private final double antibody_ratio = 0.1;
    private final int number_of_features = 1;
    private final boolean[] features_used = {true, false, false, false, false, false, false, false, false, false, false, false};
    // FEATURE_BAD_WORDS_TF, FEATURE_BAD_WORDS_TFIDF, FEATURE_NUMBER_OF_WORDS, FEATURE_POSITIVE_VS_NEGATIVE_WORDS,
    // FEATURE_NEGATION_WORDS_TF, FEATURE_EXCLUSIVE_WORDS_TF, FEATURE_SPECIAL_CHARACTERS, FEATURE_CAPITAL_LETTERS,
    // FEATURE_GRAMMAR, FEATURE_HEADLINE_WEIGHTING, FEATURE_PRECENCE_OF_NUMBERS, FEATURE_NLP = false;
    private final int k = 10;   // k-fold cross validation split


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
            antibodies[j].RR_radius = 0.1;
        }

        Antigen[][] antigens_split = new Antigen[k][(int) Math.floor(antigens.length/(float) k)];

        for (int index=0; index<k; index++) {
            System.arraycopy(antigens, index*antigens_split[index].length, antigens_split[index], 0, antigens_split[index].length);
        }


        // TODO: sett opp selve algoritmen, med cloning, mutation, affinity beregning, class assignment (classification)
        // TODO: Implementer en GUI for å se accuracy over tid og gjerne plott antibodies med RR og antigens i 2D
        // TODO: vurder om du burde slette antigensa som også har blitt til antibodies, fra antigens
        // TODO: lag noe heuristikk for RR radius initialisering


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
