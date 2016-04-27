package models;

import controllers.data.Triple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public abstract class TripleCorrupter {
    public enum EntityType {SUBJECT, OBJECT};
    protected OWLOntology ontology;
    protected OWLReasoner reasoner;
    protected final Logger logger = Logger.getLogger(TripleCorrupter.class.getName());

    public TripleCorrupter(File ontologyFile) throws OWLOntologyCreationException {
        logger.info("-- Loading ontology: " + ontologyFile.getAbsolutePath());
        ontology = OWLManager.createOWLOntologyManager().loadOntology(
                IRI.create(ontologyFile));

        logger.info("-- Initializing reasoner");
        OWLReasonerConfiguration config = new SimpleConfiguration(50000);
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    public abstract List<Triple> corrupt(Triple triple, EntityType entityType, int size);

    public static TripleCorrupter create(File ontologyFile, TripleCorrupterType tripleCorrupterType) throws OWLOntologyCreationException {
        switch (tripleCorrupterType) {
            case DISJOINT:
                return new DisjointTripleCorrupter(ontologyFile);

            default:
                throw new IllegalArgumentException("Invalid triple corrupter type!");
        }
    }
}
