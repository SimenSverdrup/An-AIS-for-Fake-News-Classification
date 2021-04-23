package Dataset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class Parser {
    public static final char DEFAULT_SEPARATOR = ',';
    public static final char DEFAULT_QUOTE_CHARACTER = '\"';
    public static final int DEFAULT_SKIP_LINES = 0;
    public static final String FAKENEWSNET_TRAIN_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\FakeNewsNet\\fake news detection(FakeNewsNet)\\fnn_train.csv";
    public static final String FAKENEWSNET_TEST_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\FakeNewsNet\\fake news detection(FakeNewsNet)\\fnn_test.csv";
    public static final String LIAR_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\LIAR\\liar_train.csv";
    public static final String IRIS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\iris.data";
    public static final String WINE_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\wine.data";
    public static final String SPIRALS_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\spirals.txt";
    public static final String DIABETES_PATH = "C:\\Users\\simen\\Documents\\A_Studier\\Masteroppgave\\Kode\\Masteropg\\Datasets\\diabetes.csv";
    public String path = "";
    private List<List<String>> data;
    public static boolean binary_class_LIAR; // convert 6-class to binary (2-class) classification for LIAR


    public Parser(Dataset dataset, int maxLines, boolean binary_class) throws FileNotFoundException {
        // Constructor for default separator (comma)

        binary_class_LIAR = binary_class;

        switch(dataset) {
            case FAKENEWSNET -> {
                this.path = FAKENEWSNET_TRAIN_PATH;
                this.data = parseFNN(this.path, maxLines);
            }
            case LIAR -> {
                this.path = LIAR_PATH;
                this.data = parseLIAR(this.path, maxLines);
            }
            case IRIS -> {
                this.path = IRIS_PATH;
                this.data = parseBenchmarkDatasets(this.path, maxLines);
                this.data.remove(this.data.size()-1); // remove last row (empty)
            }
            case WINE -> {
                this.path = WINE_PATH;
                this.data = parseBenchmarkDatasets(this.path, maxLines);
            }
            case SPIRALS -> {
                this.path = SPIRALS_PATH;
                this.data = parseBenchmarkDatasets(this.path, maxLines);
            }
            case DIABETES -> {
                this.path = DIABETES_PATH;
                this.data = parseBenchmarkDatasets(this.path, maxLines);
            }

            default -> this.path = "";
        }

    }

    public static List<List<String>> parseFNN(String path, int maxLines) throws FileNotFoundException {
        return parseCSVtoListFNN(path, DEFAULT_SEPARATOR, maxLines);
    }

    public static List<List<String>> parseLIAR(String path, int maxLines) throws FileNotFoundException {
        List<List<String>> list = parseCSVtoListLIAR(path, DEFAULT_SEPARATOR, maxLines);

        if (binary_class_LIAR) {
            List<List<String>> to_be_removed = new ArrayList<>();

            for (List<String> line : list) {
                switch (line.get(line.size() - 1)) {
                    case "barely-true", "half-true" ->
                            // remove these rows
                            to_be_removed.add(line);
                    case "pants-fire", "false" ->
                            // consider these as fake
                            line.set(line.size() - 1, "fake");
                    case "mostly-true", "true" ->
                            // consider these as real
                            line.set(line.size() - 1, "real");
                }
            }

            for (List<String> line : to_be_removed) {
                list.remove(line);
            }
        }

        return list;
    }

    public static List<List<String>> parseBenchmarkDatasets(String path, int maxLines) throws FileNotFoundException {
        InputStream input = null;
        input = new FileInputStream(path);
        BufferedReader reader = null;
        List<List<String>> CSV_list = new ArrayList<List<String>>();
        String line = null;
        StringBuilder record = new StringBuilder();
        int lineNumber = 0;

        // Parse
        try {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            while (((line = reader.readLine()) != null) && (lineNumber < maxLines)) {
                record.append(line);
                CSV_list.add(parseRecord(record.toString(), ','));
                record = new StringBuilder(); // reset
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public static List<List<String>> parseCSVtoListLIAR(String path, char separator, int maxLines) throws FileNotFoundException {
        // Parse raw CSV file to a list of lists (2D array) of Strings
        // Because records stretch over multiple lines in datasets, we have to do some extra work

        // Prepare
        InputStream input = null;
        input = new FileInputStream(path);
        BufferedReader reader = null;
        List<List<String>> CSV_list = new ArrayList<List<String>>();
        String line = null;
        StringBuilder record = new StringBuilder();
        int lineNumber = 0;

        // Parse
        try {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            while (((line = reader.readLine()) != null) && (lineNumber < maxLines )) {
                record.append(line);
                if (line.endsWith(",pants-fire") || line.endsWith(",false") || line.endsWith(",barely-true") || line.endsWith(",half-true") || line.endsWith(",mostly-true") || line.endsWith(",true") || line.endsWith(",label-liar")) {
                    // end of record
                    CSV_list.add(parseRecord(record.toString(), ','));
                    record = new StringBuilder(); // reset
                    lineNumber++;
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


    public static List<List<String>> parseCSVtoListFNN(String path, char separator, int maxLines) throws FileNotFoundException {
        // Parse raw CSV file to a list of lists (2D array) of Strings
        // Because records stretch over multiple lines in datasets, we have to do some extra work

        // Prepare
        InputStream input = null;
        input = new FileInputStream(path);
        BufferedReader reader = null;
        List<List<String>> CSV_list = new ArrayList<List<String>>();
        String line = null;
        StringBuilder record = new StringBuilder();
        int lineNumber = 0;

        // Parse
        try {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            while (((line = reader.readLine()) != null) && (lineNumber < maxLines )) {
                record.append(line);
                if (line.endsWith(",fake") || line.endsWith(",real") || line.endsWith("label_fnn")) {
                    // end of record
                    CSV_list.add(parseRecord(record.toString(), ','));
                    record = new StringBuilder(); // reset
                    lineNumber++;
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
