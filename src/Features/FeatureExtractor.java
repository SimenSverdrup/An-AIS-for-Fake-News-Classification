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
    public static final String MODAL_ADVERBS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\wiktionarylists\\modal_adverbs.txt";
    public static final String ACTION_ADVERBS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\wiktionarylists\\act_adverbs.txt";
    public static final String MANNER_ADVERBS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\wiktionarylists\\manner_adverbs.txt";
    public static final String STRONG_SUPERLATIVES_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\wiktionarylists\\superlative_forms.txt";
    public static final String COMPARATIVES_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\wiktionarylists\\comparative_forms.txt";
    public static final String NEGATIONS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\negations.txt";
    public static final String NEGATIVE_OPINION_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\negative-words.txt";
    public static final String STRONGLY_SUBJECTIVE_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\subjectivity_clues\\subjclues.tff";
    public static final String POSITIVE_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\positive-words.txt";


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
            antigens = secondPersonTF(antigens, index);
            index++;
        }
        if (this.features[3]) {
            antigens = TF(antigens, index, MODAL_ADVERBS_PATH);
            index++;
        }
        if (this.features[4]) {
            antigens = TF(antigens, index, ACTION_ADVERBS_PATH);
            index++;
        }
        if (this.features[5]) {
            antigens = FirstPersonTF(antigens, index);
            index++;
        }
        if (this.features[6]) {
            antigens = TF(antigens, index, MANNER_ADVERBS_PATH);
            index++;
        }
        if (this.features[7]) {
            antigens = TF(antigens, index, STRONG_SUPERLATIVES_PATH);
            index++;
        }
        if (this.features[8]) {
            antigens = TF(antigens, index, COMPARATIVES_PATH);
            index++;
        }
        if (this.features[9]) {
            antigens = TF(antigens, index, SWEAR_WORDS_PATH);
            index++;
        }
        if (this.features[10]) {
            antigens = numbersCounter(antigens, index);
            index++;
        }
        if (this.features[11]) {
            antigens = TF(antigens, index, NEGATIONS_PATH);
            index++;
        }
        if (this.features[12]) {
            antigens = TF(antigens, index, NEGATIVE_OPINION_WORDS_PATH);
            index++;
        }
        if (this.features[13]) {
            antigens = calculateFKGradeLevel(antigens, index);
            index++;
        }
        if (this.features[14]) {
            antigens = TF(antigens, index, STRONGLY_SUBJECTIVE_PATH);
            index++;
        }
        if (this.features[15]) {
            antigens = quotationMarks(antigens, index);
            index++;
        }
        if (this.features[16]) {
            antigens = exclamationAndQuestionMarks(antigens, index);
            index++;
        }
        if (this.features[17]) {
            antigens = TF(antigens, index, POSITIVE_WORDS_PATH);
            index++;
        }
        if (this.features[18]) {
            antigens = calculateReadingEase(antigens, index);
            index++;
        }
        if (this.features[19]) {
            antigens = unreliableSources(antigens, index);
            index++;
        }
        if (this.features[20]) {
            antigens = divisiveTopics(antigens, index);
            index++;
        }

        return antigens;
    }

    public ArrayList<Antigen> TF(ArrayList<Antigen> antigens, int index, String path) {
        // Note: calculated term frequency on the tokenized and PARTLY PROCESSED raw text (not lemmatized + stop word removed)

        List<String> lexicon = this.getLexicon(path);
        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                for (String lexicon_word : lexicon) {
                    if (word.toLowerCase(Locale.ROOT).equals(lexicon_word)) matches++;
                }
            }
            for (String word : ag.processed_headline) {
                for (String lexicon_word : lexicon) {
                    if (word.toLowerCase(Locale.ROOT).equals(lexicon_word)) matches += 2; // consider matches here to carry double the importance
                }
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }


    public ArrayList<Antigen> calculateFKGradeLevel(ArrayList<Antigen> antigens, int index) {
        // Calculate the Flesch-Kincaid Grade level
        int total_syllables = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                total_syllables += getSyllables(word);
            }
            ag.feature_list[index] = 0.39 * ((float) (ag.tokenized_and_partly_processed_text.size()/ag.sentence_count)) +
                    11.8 * ((float) (total_syllables/ag.tokenized_and_partly_processed_text.size()))
                    - 15.59;
            total_syllables = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> calculateReadingEase(ArrayList<Antigen> antigens, int index) {
        // Calculate the Flesch Reading Ease
        int total_syllables = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_text) {
                total_syllables += getSyllables(word);
            }
            ag.feature_list[index] = 206.835 - (1.015 * ((float) (ag.tokenized_text.size()/ag.sentence_count)))
                    - (84.6 * ((float) (total_syllables/ag.tokenized_text.size())));

            total_syllables = 0;
        }

        return antigens;
    }


    public ArrayList<Antigen> numbersCounter(ArrayList<Antigen> antigens, int index) {
        // Calculates occurrence of numbers

        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                if (word.matches("-?\\d+(\\.\\d+)?")) matches++;
                //match a number with optional '-' and decimal.
            }
            for (String word : ag.processed_headline) {
                if (word.matches("-?\\d+(\\.\\d+)?")) matches += 2; // consider matches here to carry double the importance
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> quotationMarks(ArrayList<Antigen> antigens, int index) {
        // Finds occurrences of quotation marks

        int matches = 0;
        String[] quote_marks = {"\"", "'", "‘", "”"};

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_text) {
                for (String token : quote_marks) {
                    if (word.equals(token)) {
                        matches++;
                    }
                }
            }
            for (String word : ag.processed_headline) {
                for (String token : quote_marks) {
                    if (word.equals(token)) {
                        matches += 2; // consider matches here to carry double the importance
                    }
                }
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> exclamationAndQuestionMarks(ArrayList<Antigen> antigens, int index) {
        // Finds occurrences of quotation marks

        int matches = 0;
        String[] tokens = {"!", "?", "!!", "!?", "?!", "??", "!!!", "???"};

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_text) {
                for (String token : tokens) {
                    if (word.equals(token)) {
                        matches++;
                    }
                }
            }
            for (String word : ag.processed_headline) {
                for (String token : tokens) {
                    if (word.equals(token)) {
                        matches += 2; // consider matches here to carry double the importance
                    }
                }
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }


    public ArrayList<Antigen> unreliableSources(ArrayList<Antigen> antigens, int index) {
        // Finds occurrences of unreliable sources in speaker field

        String[] unreliable_sources = {"youtube", "video", "email", "e-mail", "posts", "facebook", "bloggers", "twitter"};

        for (Antigen ag : antigens) {
            String[] speaker = ag.speaker.toLowerCase(Locale.ROOT).split(" ");
            ag.feature_list[index] = 0;

            for (String word : speaker) {
                for (String token : unreliable_sources) {
                    if (word.equals(token)) {
                        ag.feature_list[index] = 1;
                        break;
                    }
                }
            }
        }

        return antigens;
    }

    public ArrayList<Antigen> divisiveTopics(ArrayList<Antigen> antigens, int index) {
        // Finds occurrences of unreliable sources in speaker field

        int matches = 0;
        String[] divisive_topics = {"vaccine", "syria", "truth", "freedom", "trump", "liberals", "immigrant", "immigrants", "transgender", "marijuana", "weed", "drugs", "supremacy", "black", "white", "gay", "gun", "control", "climate", "capitalism", "privacy", "abortion", "religion", "muslim", "islam", "gender"};

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_text) {
                for (String token : divisive_topics) {
                    if (word.equals(token)) {
                        matches++;
                    }
                }
            }
            for (String word : ag.processed_headline) {
                for (String token : divisive_topics) {
                    if (word.toLowerCase(Locale.ROOT).equals(token)) {
                        matches += 2; // consider matches here to carry double the importance
                    }
                }
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> FirstPersonTF(ArrayList<Antigen> antigens, int index) {
        // Note: calculated term frequency of first person singular ("I") on the tokenized and PARTLY PROCESSED raw text (not lemmatized + stop word removed)

        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                if (word.toLowerCase(Locale.ROOT).equals("i")) matches++;
            }
            for (String word : ag.processed_headline) {
                if (word.toLowerCase(Locale.ROOT).equals("i")) matches += 2; // consider matches here to carry double the importance
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

            matches = 0;
        }

        return antigens;
    }

    public ArrayList<Antigen> secondPersonTF(ArrayList<Antigen> antigens, int index) {
        // Note: calculated term frequency of second person singular on the tokenized and PARTLY PROCESSED raw text (not lemmatized + stop word removed)

        String [] second_person_pronouns = {"you", "your", "yours", "yourself", "yourselves"};
        int matches = 0;

        for (Antigen ag : antigens) {
            for (String word : ag.tokenized_and_partly_processed_text) {
                for (String pronoun : second_person_pronouns) {
                    if (word.toLowerCase(Locale.ROOT).equals(pronoun)) matches++;

                }
            }
            for (String word : ag.processed_headline) {
                for (String pronoun : second_person_pronouns) {
                    if (word.toLowerCase(Locale.ROOT).equals(pronoun)) matches++;

                }
            }
            ag.feature_list[index] = 10*matches/(double) (ag.tokenized_and_partly_processed_text.size() + ag.processed_headline.size());

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

    public int getSyllables(String word) {
        // Calculates amount of syllables in the input word

        int syllable_count = 0;
        String vowels = "aeiouy";

        if (vowels.indexOf(word.charAt(0)) != -1) syllable_count++;

        for (int i=1; i<word.length(); i++) {
            if ((vowels.indexOf(word.charAt(i)) != -1) && (vowels.indexOf(word.charAt(i-1)) == -1)) syllable_count++;
        }

        if (word.charAt(word.length() - 1) == 'e') syllable_count -= 1;

        if (word.length() > 2) {
            if ((word.substring(word.length() - 2, word.length() - 1).equals("le")) && (vowels.indexOf(word.charAt(word.length() - 3)) == -1)) syllable_count++;
        }

        if (syllable_count == 0) syllable_count++;

        return syllable_count;
    }

    public List<String> getLexicon(String path) {
        LexiconParser lexicon_parser = new LexiconParser();
        return lexicon_parser.parse(path);
    }
}
