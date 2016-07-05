package conf;

import com.google.inject.Inject;
import com.google.inject.Provider;
import models.TripleCorrupter;
import models.TripleCorrupterType;
import ninja.utils.NinjaProperties;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;

/**
 * Class which represents a provider used to generate an instance of TripleCorrupter.
 */
public class TripleCorrupterProvider implements Provider<TripleCorrupter> {
    private final NinjaProperties properties;

    /**
     * Constructor which receives a NinjaProperties object which must contain the following parameters:
     * <ul>
     * <li>application.ontology_filename: filename of ontology</li>
     * <li>application.corrupter_id: identifier of triple corrupter (DISJOINT or SIMILARITY)</li>
     * </ul>
     *
     * @param properties Configuration parameters
     */
    @Inject
    public TripleCorrupterProvider(NinjaProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a TripleCorrupter instance using the given parameters.
     *
     * @return TripleCorrupter instance if it was created correctly, null otherwise
     */
    @Override
    public TripleCorrupter get() {
        try {
            return TripleCorrupter.create(new File(properties.get("application.ontology_filename")),
                    TripleCorrupterType.valueOf(properties.get("application.corrupter_id")));
        } catch (OWLOntologyCreationException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
