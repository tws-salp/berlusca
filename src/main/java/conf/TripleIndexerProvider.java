package conf;

import com.google.inject.Inject;
import com.google.inject.Provider;
import models.TripleIndexer;
import ninja.utils.NinjaProperties;

import java.io.File;
import java.io.IOException;

/**
 * Class which represents a provider used to generate an instance of TripleIndexer.
 */
public class TripleIndexerProvider implements Provider<TripleIndexer> {
    private final NinjaProperties properties;

    /**
     * Constructor which receives a NinjaProperties object which must contain the following parameters:
     * <ul>
     * <li>application.entity_mappings_filename: filename of entity mappings</li>
     * <li>application.relation_mappings_filename: filename of relation mappings</li>
     * </ul>
     *
     * @param properties Configuration parameters
     */
    @Inject
    public TripleIndexerProvider(NinjaProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a TripleIndexer instance using the given parameters.
     *
     * @return TripleIndexer instance if it was created correctly, null otherwise
     */
    @Override
    public TripleIndexer get() {
        try {
            return new TripleIndexer(
                    new File(properties.get("application.entity_mappings_filename")),
                    new File(properties.get("application.relation_mappings_filename")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
