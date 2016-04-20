package conf;

import com.google.inject.Inject;
import com.google.inject.Provider;
import models.TripleCorrupter;
import models.TripleCorrupterType;
import ninja.utils.NinjaProperties;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;

public class TripleCorrupterProvider implements Provider<TripleCorrupter> {

    private final NinjaProperties properties;

    @Inject
    public TripleCorrupterProvider(NinjaProperties properties) {
        this.properties = properties;
    }

    @Override
    public TripleCorrupter get() {
        try {
            System.out.println("STO CREANDOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
            return TripleCorrupter.create(new File(properties.get("application.ontology_filename")),
                    TripleCorrupterType.valueOf(properties.get("application.corrupter_id")));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        return null;
    }
}
