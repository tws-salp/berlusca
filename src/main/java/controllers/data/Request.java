package controllers.data;

import java.util.List;

public class Request {
    public List<Triple> triples;
    public int size;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("");

        builder.append("Number of corrupted triples: ").append(size).append("\n");
        builder.append("Triples: ").append("\n");

        for (Triple t : triples) {
            builder.append("\t").append(t.subject).append("\n");
            builder.append("\t").append(t.predicate).append("\n");
            builder.append("\t").append(t.object).append("\n");
            builder.append("\n");
        }

        return builder.toString();
    }
}
