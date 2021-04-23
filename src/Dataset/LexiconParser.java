package Dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LexiconParser {
    public String path = "";
    public String[] lexicon;
    public final int length = 1383;

    public String[] parse(String path) {
        this.path = path;
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
