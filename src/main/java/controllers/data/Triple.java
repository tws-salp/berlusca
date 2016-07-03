package controllers.data;

/**
 * Class which represents a triple composed by subject, predicate and object.
 */
public class Triple {
    public String subject;
    public String predicate;
    public String object;

    @Override
    public String toString() {
        return "Triple{" +
                "subject='" + subject + '\'' +
                ", predicate='" + predicate + '\'' +
                ", object='" + object + '\'' +
                '}';
    }
}
