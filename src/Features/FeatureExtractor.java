package Features;

import AIS.Antigen;
import Dataset.LexiconParser;


public class FeatureExtractor {
    // Class for extracting feature values, which features to extract is specified in the constructor

    public String[] lexicon;
    public int number_of_features = 0;

    public boolean FEATURE_BAD_WORDS_TF = false;
    public boolean FEATURE_BAD_WORDS_TFIDF = false;
    public boolean FEATURE_NUMBER_OF_WORDS = false;
    public boolean FEATURE_POSITIVE_VS_NEGATIVE_WORDS = false;  // Using Bing Liu's Opinion Lexicon
    public boolean FEATURE_NEGATION_WORDS_TF = false;   // E.g.: no, not
    public boolean FEATURE_EXCLUSIVE_WORDS_TF = false;  //E.g.: without, but, however
    public boolean FEATURE_SPECIAL_CHARACTERS = false;    // Question marks, exclamation points (note, must change the raw text parsing in Antigen to not remove these)
    public boolean FEATURE_CAPITAL_LETTERS = false;       // Same problem as above
    public boolean FEATURE_GRAMMAR = false;     // CoreNLP
    public boolean FEATURE_HEADLINE_WEIGHTING = false;
    public boolean FEATURE_PRECENCE_OF_NUMBERS = false;
    public boolean FEATURE_NLP = false;  // Sentiment analysis: BERT, Word2Vec or ELMO

    public FeatureExtractor(boolean[] features) {
        // Constructor, sets which features to use

        if (features[0]) this.FEATURE_BAD_WORDS_TF = true;
        if (features[1]) this.FEATURE_BAD_WORDS_TFIDF = true;
        if (features[2]) this.FEATURE_NUMBER_OF_WORDS = true;
        if (features[3]) this.FEATURE_POSITIVE_VS_NEGATIVE_WORDS = true;
        if (features[4]) this.FEATURE_NEGATION_WORDS_TF = true;
        if (features[5]) this.FEATURE_EXCLUSIVE_WORDS_TF = true;
        if (features[6]) this.FEATURE_SPECIAL_CHARACTERS = true;
        if (features[7]) this.FEATURE_CAPITAL_LETTERS = true;
        if (features[8]) this.FEATURE_GRAMMAR = true;
        if (features[9]) this.FEATURE_HEADLINE_WEIGHTING = true;
        if (features[10]) this.FEATURE_PRECENCE_OF_NUMBERS = true;
        if (features[11]) this.FEATURE_NLP = true;

        // TODO: sjekk prosjektoppgaven for flere features og legg til

        for (boolean status : features) {
            if (status) this.number_of_features++;
        }
    }

    public Antigen[] extractFeatures(Antigen[] antigens) {
        // Method for extracting features
        int index = 0;

        if (this.FEATURE_BAD_WORDS_TF) {
            antigens = TF(antigens, index);
            index++;
            assert(index < this.number_of_features);
        }
        if (this.FEATURE_BAD_WORDS_TFIDF) {
            antigens = TFIDF(antigens, index);
            index++;
            assert(index < this.number_of_features);
        }
        if (this.FEATURE_NUMBER_OF_WORDS) {
            antigens = wordCount(antigens, index);
            index++;
            assert(index < this.number_of_features);
        }

        return antigens;
    }

    public Antigen[] TF(Antigen[] antigens, int index) {
        this.getLexicon();
        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenizedText) {
                for (String blacklisted_word : this.lexicon) {
                    if (word.equals(blacklisted_word)) matches++;
                }
            }
            ag.TF = matches/(double) ag.tokenizedText.length;
            ag.feature_list[index] = ag.TF;

            matches = 0;
        }

        return antigens;
    }

    public Antigen[] TFIDF(Antigen[] antigens, int index) {


        return antigens;
    }

    public Antigen[] wordCount(Antigen[] antigens, int index) {
        // An extremely simple feature simply counting the words in the raw text

        for (Antigen ag : antigens) {
            ag.word_count = ag.tokenizedText.length;
            ag.feature_list[index] = ag.word_count;
        }

        return antigens;
    }

    public void getLexicon() {
        LexiconParser lexicon_parser = new LexiconParser();
        this.lexicon = lexicon_parser.parse();
    }
}
