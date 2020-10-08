import Dataset.Parser;
import static Dataset.Dataset.*;

import java.io.FileNotFoundException;
import java.util.List;


public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Parser parser = new Parser(FNN);

        List<List<String>> list = parser.getData();

        System.out.print(list.get(0) + "\n");
        System.out.print(list.get(40) + "\n");
    }
}
