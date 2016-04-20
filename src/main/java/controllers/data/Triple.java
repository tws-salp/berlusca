package controllers.data;

public class Triple {
    public String subject;
    public String predicate;
    public String object;

    public void init(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
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
