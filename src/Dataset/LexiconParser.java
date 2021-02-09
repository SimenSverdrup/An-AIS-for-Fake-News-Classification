package Dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LexiconParser {
    public static final String BAD_WORDS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\bad-words.txt";
    public String path = "";
    public String[] lexicon;
    public final int length = 1383;

    public String[] parse() {
        this.path = BAD_WORDS_PATH;
        this.lexicon = new String[length];

        int i = 0;
        try {
            File myObj = new File(this.path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                this.lexicon[i] = myReader.nextLine();
                i++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return lexicon;
    }

}
