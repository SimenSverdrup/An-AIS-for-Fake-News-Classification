package Features;

import AIS.Antigen;
import Dataset.LexiconParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FeatureExtractor {
    // Class for extracting feature values, which features to extract is specified in the constructor
    public boolean[] features;
    public int number_of_features = 0;
    public static final String BAD_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\bad-words.txt"; // long
    public static final String SWEAR_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\swear-words.txt"; // quite short
    public static final String SECOND_PERSON_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\2nd-person.txt";



    public FeatureExtractor(boolean[] features) {
        // Constructor, sets which features to use

        this.features = features;

        for (boolean status : features) {
            if (status) this.number_of_features++;
        }
    }

    public ArrayList<Antigen> extractFeatures(ArrayList<Antigen> antigens) {
        // Method for extracting features
        int index = 0;

        if (this.features[0]) {
            antigens = TF(antigens, index, BAD_WORDS_PATH);
            index++;
        }
        if (this.features[1]) {
            antigens = wordCount(antigens, index);
            index++;
        }
        if (this.features[2]) {
            antigens = TF(antigens, index, SECOND_PERSON_PATH);
            index++;
        }


        return antigens;
    }

    public ArrayList<Antigen> TF(ArrayList<Antigen> antigens, int index, String path) {
        // Note: calculated term frequency on the tokenized and PARTLY PROCESSED raw text (not lemmatized + stop word removed)

        String[] lexicon = this.getLexicon(path);
        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                for (String lexicon_word : lexicon) {
                    if (word.toLowerCase(Locale.ROOT).equals(lexicon_word)) matches++;
                }
            }
            ag.feature_list[index] = matches/(double) ag.tokenized_and_partly_processed_text.size();

            matches = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> TFIDF(ArrayList<Antigen> antigens, int index, String path) {


        return antigens;
    }

    public ArrayList<Antigen> wordCount(ArrayList<Antigen> antigens, int index) {
        // An extremely simple feature simply counting the words in the raw text

        for (Antigen ag : antigens) {
            ag.word_count = ag.tokenized_and_partly_processed_text.size();
            ag.feature_list[index] = ag.word_count;
        }

        return antigens;
    }

    public String[] getLexicon(String path) {
        LexiconParser lexicon_parser = new LexiconParser();
        return lexicon_parser.parse(path);
    }
}
