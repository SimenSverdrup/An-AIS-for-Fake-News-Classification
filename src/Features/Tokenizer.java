package Features;


import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.*;

public class Tokenizer {

    public final String[] unwanted_characters = {",", ".", "-", "--", ":", "\"", "’s", "’", "(", ")", "'", "'s", ";"};
    public final String[] abbreviations = {"U.S.", "D.C."};
    public static final String STOP_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\Lexicons\\stop-words.txt";
    public static final String[] stop_words = {"a", "about", "above", "am", "an", "and", "any", "are", "aren", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "by", "can", "couldn", "couldn't", "d", "did", "didn", "didn't", "do", "does", "doesn", "doesn't", "doing", "don", "don't", "down", "during", "for", "further", "had", "hadn", "hadn't", "has", "hasn", "hasn't", "have", "haven", "haven't", "having", "he", "her", "here", "hers", "his", "how", "if", "in", "into", "is", "isn", "isn't", "it", "it's", "its", "itself", "ll", "m", "ma", "me", "mightn", "mightn't", "most", "mustn", "mustn't", "needn", "needn't", "now", "of", "on", "once", "only", "or", "other", "re", "s", "same", "shan", "shan't", "she", "she's", "should", "should've", "shouldn", "shouldn't", "so", "some", "such", "t", "than", "that", "that'll", "the", "then", "there", "these", "this", "those", "through", "to", "too", "under", "until", "up", "ve", "was", "wasn", "wasn't", "we", "were", "weren", "weren't", "what", "when", "where", "which", "while", "who", "whom", "will", "with", "how's", "there's", "what's", "when's", "where's", "who's"};


    public List<String> tokenizeText(String raw_text) {
        // ONLY tokenize the input text

        // set up pipeline properties
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize");

        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = pipeline.processToCoreDocument(raw_text);

        List<String> tokenized_text = new ArrayList<>();

        for (CoreLabel tok : document.tokens()) {
            tokenized_text.add(tok.word());
        }

        return tokenized_text;
    }

    public List<String> tokenizeAndProcessText(String raw_text) {
        // Tokenize, lemmatize the input text and remove stop words

        // set up pipeline properties
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");

        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = pipeline.processToCoreDocument(raw_text);

        List<String> tokenized_and_processed_text = new ArrayList<>();
        //LexiconParser lexicon = new LexiconParser();
        //String[] stop_words = lexicon.parse(STOP_WORDS_PATH);

        for (CoreLabel tok : document.tokens()) {
            // Lemmatize + remove unwanted characters and stop words
            String word = tok.word();
            if (Arrays.stream(stop_words).noneMatch(word.toLowerCase(Locale.ROOT)::equals)) {
                if (Arrays.stream(unwanted_characters).noneMatch(word::equals)) {
                    String lem = tok.lemma();
                    String[] lem_arr = lem.split("\\.");
                    if ((lem_arr.length > 1) && (Arrays.stream(abbreviations).noneMatch(lem::equals))) {
                        tokenized_and_processed_text.add(lem_arr[0]);
                        tokenized_and_processed_text.add(lem_arr[1]);
                    }
                    else if (lem.equals("n't") || word.equals("n't")) {
                        tokenized_and_processed_text.add("not");
                    }
                    else {
                        tokenized_and_processed_text.add(lem);
                    }
                }
            }
        }

        return tokenized_and_processed_text;
    }

}
