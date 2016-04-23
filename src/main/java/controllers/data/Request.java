package controllers.data;

import java.util.List;

public class Request {
    public List<Triple> triples;
    public int numCorrupted;

    @Override
    public String toString() {
        return "Request{" +
                "triples=" + triples +
                ", numCorrupted=" + numCorrupted +
                '}';
    }
}
