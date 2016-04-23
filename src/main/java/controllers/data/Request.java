package controllers.data;

import java.util.List;

public class Request {
    public List<Triple> triples;
    public int size;

    @Override
    public String toString() {
        return "Request{" +
                "triples=" + triples +
                ", size=" + size +
                '}';
    }
}
