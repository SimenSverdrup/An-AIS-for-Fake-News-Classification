package Dataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Parser {
    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUOTE_CHARACTER = '\"';
    public static final int DEFAULT_SKIP_LINES = 0;
    public static final String FNN_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Prosjektoppgave\\Kode\\IMAIS-FNC\\Datasets\\FNID dataset\\fake news detection(FakeNewsNet)\\fnn_train.csv";
    public static final String LIAR_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Prosjektoppgave\\Kode\\IMAIS-FNC\\Datasets\\FNID dataset\\fake news detection(LIAR)\\liar_train.csv";
    public static final String KAGGLE_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Prosjektoppgave\\Kode\\IMAIS-FNC\\Datasets\\Kaggle Fake News\\train.csv";
    public String path = "";

    // Remember that the first line of data only contains the headers!
    private List<List<String>> data;


    public Parser(Dataset dataset) throws FileNotFoundException {
        // Constructor for default separator (comma)

        switch(dataset) {
            case FNN -> this.path = FNN_PATH;
            case LIAR -> this.path = LIAR_PATH;
            case KAGGLE -> this.path = KAGGLE_PATH;
            default -> this.path = "";
        }

        this.data = parseCSVtoList(this.path, DEFAULT_SEPARATOR);
    }


    public static List<List<String>> parseCSVtoList(String path, char separator) throws FileNotFoundException {
        // Parse raw CSV file to a list of lists (2D array) of Strings
        // Each row is an individual article text + label

        // Prepare
        InputStream input = null;
        input = new FileInputStream(path);
        BufferedReader reader = null;
        List<List<String>> CSV_list = new ArrayList<List<String>>();
        String line = null;
        StringBuilder record = new StringBuilder();

        // Parse
        try {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            while ((line = reader.readLine()) != null) {
                record.append(line);
                if (line.endsWith(",fake") || line.endsWith(",real") || line.endsWith("label_fnn")) {
                    // end of record
                    CSV_list.add(parseRecord(record.toString(), separator));
                    record = new StringBuilder(); // reset
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV parsing failed.", e);
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return CSV_list;
    }


    private static List<String> parseRecord(String line, char separator) {
        // From http://www.java2s.com/example/java-utility-method/csv-file-parse/parsecsv-inputstream-csvinput-3765b.html

        boolean quoted = false;
        boolean square_brackets = false;
        StringBuilder fieldBuilder = new StringBuilder();
        List<String> parsed_line = new ArrayList<>();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            fieldBuilder.append(c);

            if (c == '"') {
                quoted = !quoted; // Detect nested quotes.
            }
            else if (c == '[') {
                square_brackets = true; // Detect nested quotes.
            }
            else if (c == ']') {
                square_brackets = false; // Detect nested quotes.
            }


            if ((!quoted && !square_brackets && c == separator) // The separator ..
                    || i + 1 == line.length()) // .. or, the end of record.
            {
                String field = fieldBuilder.toString() // Obtain the field, ..
                        .replaceAll(separator + "$", "") // .. trim ending separator, ..
                        .replaceAll("^\"|\"$", "") // .. trim surrounding quotes, ..
                        .replace("\"\"", "\""); // .. and un-escape quotes.
                parsed_line.add(field.trim()); // Add field to List.
                fieldBuilder = new StringBuilder(); // Reset.
            }

        }
        return parsed_line;
    }

    public List<List<String>> getData() {
        return data;
    }
}
