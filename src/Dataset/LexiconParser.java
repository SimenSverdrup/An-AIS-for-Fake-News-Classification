package Dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static Features.FeatureExtractor.STRONGLY_SUBJECTIVE_PATH;

public class LexiconParser {
    public String path = "";
    public List<String> lexicon;

    public List<String> parse(String path) {
        this.path = path;
        this.lexicon = new ArrayList<>();

        try {
            File myObj = new File(this.path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                this.lexicon.add(myReader.nextLine());
                if (path.equals(STRONGLY_SUBJECTIVE_PATH)) {
                    String line = myReader.nextLine();
                    if (line.startsWith("type=strongsubj")) {
                        // We only want the strongly subjective terms
                        String word = "";
                        int i = 28;
                        while (line.charAt(i) != ' ') i++;
                        this.lexicon.add(line.substring(28, i));
                    }
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return lexicon;
    }

}
