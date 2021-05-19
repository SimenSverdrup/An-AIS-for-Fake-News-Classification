package AIS;

import Dataset.Dataset;
import Dataset.Parser;
import Features.FeatureExtractor;
import Features.Hasher;
import Features.Normaliser;
import GUI.GUI;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static Dataset.Dataset.*;


public class Controller {
    public double total_accuracy;
    public double[] accuracies;
    public double[][] generation_accuracies;
    public double[] final_generation_accuracies;
    public Antigen[] antigens; // all antigens
    public ArrayList<Antibody> antibodies;
    public boolean negative_vals = false;

    public Antigen[][] antigens_split; // antigens split into k separate arrays, for k-fold cross-validation testing
    public ArrayList<Antigen> training_antigens; // antigens used for training (this iteration)
    public ArrayList<Antigen> testing_antigens; // antigens used for testing (this iteration)
    public ArrayList<Antibody> worst_performing_abs;

    public final int k = 3;   // k-fold cross validation split
    public final double antibody_ratio = 1.0;
    public final double max_antibody_replacement_ratio = 0.1;
    public final int number_of_clones = 10; // number of clones per antibody
    public final double antibody_replacement_decrease_factor = 1.5; // skriver at denne er statisk lik 1.5 i overleafen
    public double feature_vector_mutation_probability;
    public double RR_radius_mutation_probability;
    public final double antigen_initialised_ratio = 0.5; // ratio of antibodies initialised with antigen feature vectors
    public final double randomly_initialised_ratio = 0.5; // ratio of antibodies initialised with random feature vectors
    public final int generations = 300;
    public final double antibody_removal_threshold = 0.01; // the fitness value threshold for removing antibodies

    public final boolean plot_testing_set = false; // false for plotting training set instead
    public final boolean VALIS_RR_radius_init_scheme = true;
    public final Dataset dataset = KAGGLE; //KAGGLE //FAKENEWSNET //LIAR //IRIS //SPIRALS //WINE //DIABETES (Pima Indian) //SONAR
    public int number_of_features = 13; // IRIS=4, SPIRALS=2, WINE=13, DIABETES=8, SONAR=60
    public final boolean binary_class_LIAR = true;
    public final int max_lines = 50;

    private final boolean[] features_used = {
            // TF features are weighted double if found in the headline (headline weighting inspired by 3HAN)
            false, // Word count - Newman et al. (2003)
            false, // 2nd person TF - "Truth of varying shades" (Rashkin et al.) + \citep{FakeNewsRumors}
            false, // Modal adverbs - "Truth of varying shades" (Rashkin et al.)
            false, // Action adverbs - "Truth of varying shades" (Rashkin et al.)
            false, // 1st pers singular (I) - "Truth of varying shades" (Rashkin et al.)
            false, // Manner adverbs  - "Truth of varying shades" (Rashkin et al.)
            false, // Superlatives - "Truth of varying shades" (Rashkin et al.)
            false, // Comparative forms - "Truth of varying shades" (Rashkin et al.)
            true, // Swear words - "Truth of varying shades" (Rashkin et al.) + Bad words TF - https://www.cs.cmu.edu/~biglou/resources/
            false, // Numbers - "Truth of varying shades" (Rashkin et al.) + \citep{FakeNewsRumors}
            false, // Negations - "Behind the cues" (made myself)
            false, // Negative opinion words - "Behind the cues" (Gravanis et al.) + lexicon from "Mining and Summarizing Customer Reviews." (Minqing Hu and Bing Liu)
            true, // Flesch-Kincaid Grade level - "Behind the cues" (Gravanis et al.)
            false, // Strongly subjective words - (MPQA)
            false, // Quotation marks TF (found from manual review of the articles)
            false, // Exclamation + question marks TF (\citep{FakeNewsRumors})
            false, // Positive words - "Behind the cues" (Gravanis et al.) + lexicon from "Mining and Summarizing Customer Reviews." (Minqing Hu and Bing Liu)
            false, // Flesch Reading Ease - \citep{linguistic-feature-based}
            false, // Unreliable sources, in speaker field (binary, not TF) - "Behind the cues" (Gravanis et al.) + made lexicon myself based on findings of Gravanis et al. (facebook posts, bloggers etc.)
            false, // Divisive topics - "Behind the cues" (Gravanis et al.) + made lexicon myself based on findings of Gravanis et ag.
            false, // Google Fact Check API hash value - Found myself
            false, // BERT for word embeddings for HEADLINE ONLY - see NLP processing in State of the art for inspiration (Fakeddit) (https://zenodo.org/record/2652964#.YJE2wbUzY2w)
            false, // BERT for word embeddings for FULL TEXT (first and last sentence) - see NLP processing in State of the art for inspiration (Fakeddit) (https://zenodo.org/record/2652964#.YJE2wbUzY2w)
            false, // BERT for word embeddings for HEAD (first sentence) - see NLP processing in State of the art for inspiration (Fakeddit)
            false, // BERT for word embeddings for TAIL (last sentence) - see NLP processing in State of the art for inspiration (Fakeddit)
            true, // Sentiment Analysis of headline with Stanford CoreNLP - Scores from 0-4 based on resulting in: Very Negative, Negative, Neutral, Positive or Very Positive, respectively
            false, // Sentiment Analysis of head and tail of article text with Stanford CoreNLP - Scores from 0-4 based on resulting in: Very Negative, Negative, Neutral, Positive or Very Positive, respectively
    };

    public void run() throws Exception {
        Parser parser = new Parser(this.dataset, this.max_lines + 1, this.binary_class_LIAR); // + 1 because first line are headers only

        List<List<String>> list = parser.getData();

        if (this.dataset.equals(FAKENEWSNET) || this.dataset.equals(LIAR) || this.dataset.equals(KAGGLE)) {
            list.remove(0); // remove headers
            this.number_of_features = 0;

            for (boolean b : features_used) {
                if (b) {
                    this.number_of_features++;
                }
            }
            if (features_used[21]) {
                // 768-field text embeddings are used
                this.number_of_features += 767;
                this.negative_vals = true;
            }
            if (features_used[22]) {
                // 768-field text embeddings are used
                this.number_of_features += 767;
                this.negative_vals = true;
            }
            if (features_used[23]) {
                // 768-field text embeddings are used
                this.number_of_features += 767;
                this.negative_vals = true;
            }
            if (features_used[24]) {
                // 768-field text embeddings are used
                this.number_of_features += 767;
                this.negative_vals = true;
            }
        }

        this.feature_vector_mutation_probability = 1.0/ (double) (1 + this.number_of_features);
        this.RR_radius_mutation_probability = 1.0/ (double) (1 + this.number_of_features);
        int number_of_records = list.size();
        this.antigens = new Antigen[number_of_records];
        this.training_antigens = new ArrayList<>();
        this.testing_antigens = new ArrayList<>();
        this.antibodies = new ArrayList<>();
        this.worst_performing_abs = new ArrayList<>();
        this.generation_accuracies = new double[k][this.generations];
        this.final_generation_accuracies = new double[this.generations];

        int i = 0;
        for (List<String> record : list) {
            this.antigens[i] = new Antigen(record, number_of_features, this.dataset, this.binary_class_LIAR);
            i++;
            /*
            if (this.binary_class_LIAR && (this.dataset == LIAR)) {
                if (!(record.get(record.size() - 1).equals("barely-true") || record.get(record.size() - 1).equals("half-true"))) {
                    // for the LIAR dataset, disregard samples with the two "middle" labels
                    this.antigens[i] = new Antigen(record, number_of_features, this.dataset, this.binary_class_LIAR);
                    i++;
                }
            }
            else {
                this.antigens[i] = new Antigen(record, number_of_features, this.dataset, this.binary_class_LIAR);
                i++;
            }
            */
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
            if ((this.dataset == FAKENEWSNET) || (this.dataset == LIAR) || (this.dataset == KAGGLE)) {
                this.training_antigens = fe.extractFeatures(this.training_antigens);
            }

            for (Antigen ag : this.training_antigens) {
                //System.out.println("\nClass: " + ag.true_class + "    Non-normalized feature vector: " + Arrays.toString(ag.feature_list));
                //System.out.println("Non-normalized feature vector: " + Arrays.toString(ag.feature_list));
                //System.out.println("Speaker: " + ag.speaker);
                //System.out.println("Headline: " + ag.headline);
            }
            this.training_antigens = norm.NormaliseFeatures(this.training_antigens, this.negative_vals);

            this.antibodies.clear();
            List<Antigen> antigens_added = new ArrayList<>();

            for (int j=0; j<this.training_antigens.size()*antibody_ratio*this.antigen_initialised_ratio; j++) {
                // Initialize antibodies using antigens

                int rand = 0;

                do {
                    rand = ThreadLocalRandom.current().nextInt(this.training_antigens.size());
                }
                while (antigens_added.contains(this.training_antigens.get(rand)));

                this.antibodies.add(new Antibody(this.training_antigens.get(rand)));
                antigens_added.add(this.training_antigens.get(rand));

                //if (j > this.training_antigens.size()*antibody_ratio*0.9) this.antibodies.get(this.antibodies.size()-1).random(this.training_antigens);
                // randomise feature values of added antibody
            }

            for (int j=0; j<this.training_antigens.size()*antibody_ratio*this.randomly_initialised_ratio; j++) {
                //Add some randomly initialised antibodies

                int rand = ThreadLocalRandom.current().nextInt(0, this.training_antigens.size() - 1);
                this.antibodies.add(new Antibody(this.training_antigens.get(rand)));
                this.antibodies.get(this.antibodies.size()-1).random(this.training_antigens);
                // randomise feature values of added antibody
            }

            for (Antibody ab : this.antibodies) {
                // Initialise anitbody RR radii

                ab.RR_radius = 1000;

                if (this.VALIS_RR_radius_init_scheme) {
                    Random rand = new Random();

                    while (true) {
                        // Set antibody RR radius to euclidean distance to RANDOM ag of SAME class
                        int random_index = rand.nextInt(this.training_antigens.size());

                        if (this.training_antigens.get(random_index).true_class.equals(ab.true_class)) {
                            ab.RR_radius = aff.CalculateDistance(this.training_antigens.get(random_index).feature_list, ab.feature_list, ab.features_used) + 0.001;
                            break;
                        }
                    }
                }
                else {
                    for (Antigen ag : antigens) {
                        if (!ag.true_class.equals(ab.true_class)) {
                            // Set antibody RR radius to euclidean distance to closest ag of DIFFERENT class (but not including the ag)
                            ab.RR_radius = Math.min(aff.CalculateDistance(ag.feature_list, ab.feature_list, ab.features_used) - 0.001, ab.RR_radius);
                        }
                    }
                }
            }

            ArrayList<Antibody> clones = new ArrayList<>();
            List<Antibody> abs_to_be_deleted = new ArrayList<>();

            // Begin the training
            for (int generation=1; generation<=this.generations; generation++) {
                System.out.println("Generation " + generation + "/" + (this.generations));

                clones.clear();
                abs_to_be_deleted.clear();

                double reproduction_ratio = this.max_antibody_replacement_ratio * Math.pow(2 / (double) this.antibodies.size(), (double) generation / ((double) (this.generations) * this.antibody_replacement_decrease_factor));
                int number_of_new_antibodies = (int) (reproduction_ratio * this.antibodies.size());

                for (Antigen ag : this.training_antigens) {
                    // Need to find the affinity vector for each antigen, in order to accurately calculate the fitness of each ab
                    ag.findConnectedAntibodies(this.antibodies);
                }

                for (Antibody ab : this.antibodies) {
                    ab.findConnectedAntigens(this.training_antigens);
                    ab.calculateFitness(this.training_antigens);
                    //System.out.println("\nFeatures used by this ab: " + Arrays.toString(ab.features_used));
                }

                HashMap<Integer, Double> fitnesses = new HashMap<>();

                int ab_index = 0;

                for (Antibody ab : this.antibodies) {
                    fitnesses.put(ab_index, ab.fitness);
                    ab_index++;
                }


                /*
                int number_of_classes = training_antigens.get(0).number_of_classes;
                String[] classes = training_antigens.get(0).classes;

                List<List<Antibody>> parents = new ArrayList<>();
                List<Antibody> children = new ArrayList<>();

                for (int clss=0; clss<number_of_classes; clss++) {
                    List<Antibody> class_parents = new ArrayList<>(); // the parents for this class

                    for (Antibody ab : this.antibodies) {
                        if (classes[clss].equals(ab.true_class)) {
                            class_parents.add(ab);
                        }
                    }

                    parents.add(class_parents);
                }

                int children_per_class = number_of_new_antibodies/number_of_classes;

                for (int clss=0; clss<number_of_classes; clss++) {

                    int children_left = children_per_class;

                    HashMap<Integer, Double> fitnesses1 = new HashMap<>();
                    int idx = 0;

                    for (Antibody ab : parents.get(clss)) {
                        fitnesses1.put(idx, ab.fitness);
                        idx++;
                    }

                    while ((children_left > 0) && (fitnesses1.size() > 1)) {
                        int max_key = Collections.max(fitnesses1.entrySet(), Map.Entry.comparingByValue()).getKey();
                        fitnesses1.remove(max_key);

                        int max_key2 = Collections.max(fitnesses1.entrySet(), Map.Entry.comparingByValue()).getKey();
                        fitnesses1.remove(max_key2);

                        Random rand = new Random();

                        int split1 = rand.nextInt(this.number_of_features);
                        int split2 = rand.nextInt(this.number_of_features);
                        while (split1 == split2) split2 = rand.nextInt(this.number_of_features);

                        //split after this index, nextInt is exclusive so split in range [0-(number_of_features-2)]

                        double[] new_feature_vals1 = new double[this.number_of_features];
                        double[] new_feature_vals2 = new double[this.number_of_features];

                        for (int j = 0; j < Math.min(split1, split2); j++) {
                            new_feature_vals1[j] = parents.get(clss).get(max_key).feature_list[j];
                            new_feature_vals2[j] = parents.get(clss).get(max_key2).feature_list[j];
                        }

                        for (int j = Math.min(split1, split2); j < Math.max(split1, split2); j++) {
                            new_feature_vals1[j] = parents.get(clss).get(max_key2).feature_list[j];
                            new_feature_vals2[j] = parents.get(clss).get(max_key).feature_list[j];
                        }

                        for (int j = Math.max(split1, split2); j < this.number_of_features; j++) {
                            new_feature_vals1[j] = parents.get(clss).get(max_key).feature_list[j];
                            new_feature_vals2[j] = parents.get(clss).get(max_key2).feature_list[j];
                        }

                        Antibody child1 = new Antibody(parents.get(clss).get(max_key));
                        Antibody child2 = new Antibody(parents.get(clss).get(max_key2));
                        child1.feature_list = new_feature_vals1.clone();
                        children.add(child1);

                        if (children_left > 1) {
                            child2.feature_list = new_feature_vals2.clone();
                            children.add(child2);
                        }

                        System.out.println("\nSplit1: " + split1);
                        System.out.println("Split2: " + split2);
                        System.out.println("Parent 1: " + Arrays.toString(parents.get(clss).get(max_key).feature_list));
                        System.out.println("Parent 2: " + Arrays.toString(parents.get(clss).get(max_key2).feature_list));
                        System.out.println("Child 1: " + Arrays.toString(child1.feature_list));
                        if (children_per_class > 1) System.out.println("Child 2: " + Arrays.toString(child2.feature_list));


                        for (Antibody child : children) {
                            child.mutate(1 / (1 + (double) this.number_of_features), 1 / (1 + (double) this.number_of_features));
                        }
                        children_left -= 2;
                    }
                }


                int antibodies_size_before = this.antibodies.size();
                this.antibodies.addAll(children);
                int antibodies_size_after = this.antibodies.size();

                System.out.println("ANTIBODIES SIZE: " + this.antibodies.size());
                */

                int antibody_clones_left = number_of_new_antibodies;

                while (antibody_clones_left > 0) {
                    int max_key = Collections.max(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(max_key);
                    for (int num=0; num<this.number_of_clones; num++) {
                        clones.add(new Antibody(this.antibodies.get(max_key)));
                    }

                    antibody_clones_left--;
                }

                for (Antibody clone : clones) {
                    clone.reset();
                    clone.mutate(this.feature_vector_mutation_probability, this.RR_radius_mutation_probability);
                }


                fitnesses.clear();
                ab_index = 0;
                int number_of_abs_to_be_deleted = number_of_new_antibodies;

                for (Antigen ag : this.training_antigens) {
                    // Need to find the affinity vector for each antigen, in order to accurately calculate the fitness of each ab
                    ag.findConnectedAntibodies(this.antibodies);
                }

                for (Antibody ab : this.antibodies) {
                    ab.findConnectedAntigens(this.training_antigens);
                }

                for (Antibody ab : this.antibodies) {
                    ab.calculateFitness(this.training_antigens);
                    fitnesses.put(ab_index, ab.fitness);
                    ab_index++;
                }

                while (number_of_abs_to_be_deleted > 0) {
                    // Find antibodies in this.antibodies with poorest fitness
                    int min_key = Collections.min(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(min_key);
                    abs_to_be_deleted.add(this.antibodies.get(min_key));

                    number_of_abs_to_be_deleted--;
                }

                for (Antibody ab : abs_to_be_deleted) {
                    // Remove poorest performing antibodies from this.antibodies
                    this.antibodies.remove(ab);
                }




                ArrayList<Antigen> antigens_copy = this.training_antigens;

                for (Antibody clone : clones) {
                    clone.findConnectedAntigens(antigens_copy);
                }


                fitnesses.clear();
                int clone_index = 0;
                for (Antibody clone : clones) {
                    clone.calculateFitness(antigens_copy);
                    fitnesses.put(clone_index, clone.fitness);
                    clone_index++;
                }

                List<Antibody> clones_to_be_deleted = new ArrayList<>();
                int clone_size = clones.size();

                while (clone_size > abs_to_be_deleted.size()) {
                    // Find antibodies in clones with poorest fitness
                    int min_key = Collections.min(fitnesses.entrySet(), Map.Entry.comparingByValue()).getKey();
                    fitnesses.remove(min_key);
                    clones_to_be_deleted.add(clones.get(min_key));

                    clone_size--;
                }

                for (Antibody clone : clones_to_be_deleted) {
                    clones.remove(clone);
                }

                // Add the new clones
                this.antibodies.addAll(clones);



                ///////////////// Calculate accuracy for plotting:
                if (this.plot_testing_set) {
                    this.testing_antigens.clear();
                    this.testing_antigens.addAll(Arrays.asList(this.antigens_split[k]));

                    if ((this.dataset == FAKENEWSNET) || (this.dataset == LIAR) || (this.dataset == KAGGLE)) {
                        this.testing_antigens = fe.extractFeatures(this.testing_antigens);
                    }

                    this.testing_antigens = norm.NormaliseFeatures(this.testing_antigens, this.negative_vals);

                    double correct_predictions = 0;

                    for (Antigen ag : this.testing_antigens) {
                        ag.findConnectedAntibodies(this.antibodies);
                        ag.predictClass(this.antibodies);


                        if (ag.true_class.equals(ag.predicted_class)) {
                            correct_predictions++;
                        }
                    }

                    //this.accuracies[k] = correct_predictions / this.testing_antigens.size();
                    this.generation_accuracies[k][generation-1] = correct_predictions / this.testing_antigens.size();
                }
                else {
                    //this.testing_antigens.clear();
                    //this.testing_antigens.addAll(Arrays.asList(this.antigens_split[k]));
                    //this.testing_antigens = norm.NormaliseFeatures(this.testing_antigens);

                    ArrayList<Antibody> antibodies_temp = new ArrayList<>();

                    for (Antibody ab : this.antibodies) {
                        antibodies_temp.add(new Antibody(ab));
                    }

                    for (Antibody ab : antibodies_temp) {
                        ab.findConnectedAntigens(this.training_antigens);
                    }

                    double correct_predictions = 0;

                    for (Antigen ag : this.training_antigens) {
                        ag.findConnectedAntibodies(antibodies_temp);
                        ag.predictClass(antibodies_temp);

                        if (ag.true_class.equals(ag.predicted_class)) {
                            correct_predictions++;
                        }
                    }

                    this.generation_accuracies[k][generation-1] = correct_predictions / this.training_antigens.size();
                }
                /////////////////
            }


            for (int ab_idx=0; ab_idx<this.antibodies.size(); ab_idx++) {
                // remove abs with fitness lower than threshold
                this.antibodies.get(ab_idx).findConnectedAntigens(this.training_antigens);
                this.antibodies.get(ab_idx).calculateFitness(this.training_antigens);
                //System.out.println("Ab fitness: " + this.antibodies.get(ab_idx).fitness);

                if (this.antibodies.get(ab_idx).fitness < this.antibody_removal_threshold) {
                    this.antibodies.remove(this.antibodies.get(ab_idx));
                    System.out.println("REMOVE");
                }
            }

            //-----------------Calculate accuracy, on testing set------------------
            this.testing_antigens.clear();
            this.testing_antigens.addAll(Arrays.asList(this.antigens_split[k]));

            if ((this.dataset == FAKENEWSNET) || (this.dataset == LIAR) || (this.dataset == KAGGLE)) {
                this.testing_antigens = fe.extractFeatures(this.testing_antigens);
            }

            this.testing_antigens = norm.NormaliseFeatures(this.testing_antigens, this.negative_vals);

            double correct_predictions = 0;

            for (Antigen ag : this.testing_antigens) {
                ag.findConnectedAntibodies(this.antibodies);
                ag.predictClass(this.antibodies);

                if (ag.connected_antibodies.size() == 0) {
                    System.out.println("No connected abs to this ag!");
                }

                /*System.out.println("Connected abs to this ag: " + ag.connected_antibodies.size());
                System.out.println("Ag feature list: " + Arrays.toString(ag.feature_list));
                System.out.println("Ag class: " + ag.true_class);
                System.out.println("Ag predicted class: " + ag.predicted_class);
                System.out.println("Ag class vote: " + Arrays.toString(ag.class_vote));
                System.out.println("\n");*/


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


        // Calculate accuracy for every generation, averaged for every k
        for (int gen=0; gen<this.generations; gen++) {
            for (int index=0; index<k; index++) {
                this.final_generation_accuracies[gen] += this.generation_accuracies[index][gen];
            }
            this.final_generation_accuracies[gen] = this.final_generation_accuracies[gen]/k;
        }
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
