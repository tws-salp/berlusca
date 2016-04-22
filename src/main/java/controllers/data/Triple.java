package controllers.data;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Triple {
    public String subject;
    public String predicate;
    public String object;

    public Triple decode() throws UnsupportedEncodingException {
        this.subject = URLDecoder.decode(this.subject.replace("\\", ""), "UTF-8");
        this.predicate = URLDecoder.decode(this.predicate.replace("\\", ""), "UTF-8");
        this.object = URLDecoder.decode(this.object.replace("\\", ""), "UTF-8");

        return this;
    }

    @Override
    public String toString() {
        return "Triple{" +
                "subject='" + subject + '\'' +
                ", predicate='" + predicate + '\'' +
                ", object='" + object + '\'' +
                '}';
    }
}
