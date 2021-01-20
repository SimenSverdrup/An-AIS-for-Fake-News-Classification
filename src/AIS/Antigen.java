package AIS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Antigen {
    public String true_class;
    public String id;
    public String predicted_class;
    public int number_of_connections; // number of antibodies connecting to this antigen (must be reset at every training cycle)
    public double[] feature_list; // note, length not equal to raw_list - as these are the floating feature values

    public String raw_text;
    public String[] sources;
    public String speaker;
    public String headline;

    public Antigen(List<String> record) {
        // record is a single line in the form:
        // id,date,speaker,statement,sources,paragraph_based_content,fullText_based_content,label_fnn

        this.true_class = record.get(record.size() - 1);
        this.id = record.get(0);

        this.speaker = record.get(2); // speaker
        this.headline = record.get(3); // headline
        this.raw_text = record.get(6); // full text

        this.sources = record.get(4).split(", ");
        this.sources[0] = this.sources[0].substring(1);
        int length = this.sources.length;
        this.sources[length-1] = this.sources[length-1].substring(0, this.sources[length-1].length());

        for (int i=0; i<this.sources.length; i++) {
            this.sources[i] = this.sources[i].substring(1, this.sources[i].length()-1);
            try {
                URI uri = new URI(this.sources[i]);
                String domain = uri.getHost();
                this.sources[i] = domain.startsWith("www.") ? domain.substring(4) : domain;
            }
            catch (URISyntaxException err) {
                System.out.println("Could not fetch host information");
            }
        }
    }

    public void resetNumberOfConnections() {
        this.number_of_connections = 0;
    }

    public void test() {
        assert(this.true_class.equals("fake") || this.true_class.equals("true"));
    }
}
