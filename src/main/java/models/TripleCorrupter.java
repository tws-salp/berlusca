package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Abstract class which represents a TripleCorrupter.
 */
public abstract class TripleCorrupter {
    protected OWLOntology ontology;
    protected OWLReasoner reasoner;
    protected final Random randomTripleGenerator;
    protected final Random randomEntityGenerator;
    protected Multimap<OWLNamedIndividual, OWLClass> individualsClasses;
    protected Multimap<OWLClass, OWLNamedIndividual> classesIndividuals;
    protected final Logger logger = Logger.getLogger(TripleCorrupter.class.getName());
    protected final int RANDOM_SEED = 12345;

    /**
     * Constructor which receives an ontology file to read the ontology and instantiate the reasoner.
     *
     * @param ontologyFile Ontology file to be read
     * @throws OWLOntologyCreationException Exception raised if the ontology cannot be parsed
     * @throws IOException Exception raised if the file cannot be read
     */
    public TripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        logger.info("-- Loading ontology: " + ontologyFile.getAbsolutePath());
        ontology = OWLManager.createOWLOntologyManager().loadOntology(
                IRI.create(ontologyFile));

        logger.info("-- Initializing reasoner");
        OWLReasonerConfiguration config = new SimpleConfiguration(50000);
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        this.randomTripleGenerator = new Random(RANDOM_SEED);
        this.randomEntityGenerator = new Random(RANDOM_SEED);

        logger.info("-- Building individuals to classes index");
        buildIndividualsClasses();

        logger.info("-- Building classes to individuals index");
        buildClassesIndividuals();
    }

    /**
     * Corrupts the given triple generating numCorrupted triples considering the given indexer.
     *
     * @param triple Triple to be corrupted
     * @param numCorrupted Number of corrupted triples to be generated
     * @param indexer Mapping between URIs and integer identifiers
     * @return List of corrupted triples
     */
    public List<Triple> corrupt(Triple triple, int numCorrupted, TripleIndexer indexer) {
        List<Triple> triples = new ArrayList<>();
        triple.subject = indexer.getId2entity().get(Long.parseLong(triple.subject));
        triple.predicate = indexer.getId2relation().get(Long.parseLong(triple.predicate));
        triple.object = indexer.getId2entity().get(Long.parseLong(triple.object));

        for (int i = 0; i < numCorrupted; i++) {
            Triple t = corrupt(triple, randomTripleGenerator.nextBoolean());
            Triple t_id = new Triple();
            t_id.subject = String.valueOf(indexer.getEntity2id().get(t.subject));
            t_id.predicate = String.valueOf(indexer.getRelation2id().get(t.predicate));
            t_id.object = String.valueOf(indexer.getEntity2id().get(t.object));
            triples.add(t_id);
        }

        return triples;
    }

    /**
     * Generates a corrupted triple given a triple and a boolean flag which defines which part to corrupt.
     *
     * @param triple Triple to be corrupted
     * @param corruptSubject True corrupts the subject, False corrupts the object
     * @return Corrupted triple
     */
    protected abstract Triple corrupt(Triple triple, boolean corruptSubject);

    /**
     * Factory method to instantiate the requested triple corrupter.
     *
     * @param ontologyFile Ontology file to be read
     * @param tripleCorrupterType Identifier of the triple corrupter
     * @return Requested triple corrupter
     * @throws OWLOntologyCreationException Exception raised if the ontology cannot be parsed
     * @throws IOException Exception raised if the file cannot be read
     */
    public static TripleCorrupter create(File ontologyFile, TripleCorrupterType tripleCorrupterType)
            throws OWLOntologyCreationException, IOException {
        switch (tripleCorrupterType) {
            case DISJOINT:
                return new DisjointTripleCorrupter(ontologyFile);
            case SIMILARITY:
                return new SimilarityTripleCorrupter(ontologyFile);
            default:
                throw new IllegalArgumentException("Invalid triple corrupter type!");
        }
    }

    /**
     * Generated a corrupted triple using random sampling.
     *
     * @param triple Triple to be corrupted
     * @param iriIndividual Entity to be corrupted
     * @param corruptSubject True corrupts the subject, False corrupts the object
     * @return Corrupted triple
     */
    protected Triple generateRandomTriple(Triple triple, OWLNamedIndividual iriIndividual, boolean corruptSubject) {
        Triple corruptedTriple = new Triple();
        List<OWLNamedIndividual> ontologyIndividuals = new ArrayList<>(individualsClasses.keySet());

        if (corruptSubject) {

            OWLNamedIndividual corruptedEntity;
            do {
                corruptedEntity = ontologyIndividuals.get(randomEntityGenerator.nextInt(ontologyIndividuals.size()));
            } while (corruptedEntity.equals(iriIndividual));

            corruptedTriple.subject = corruptedEntity.getIRI().toString();
            corruptedTriple.predicate = triple.predicate;
            corruptedTriple.object = triple.object;
        } else {
            OWLNamedIndividual corruptedEntity;
            do {
                corruptedEntity = ontologyIndividuals.get(randomEntityGenerator.nextInt(ontologyIndividuals.size()));
            } while (corruptedEntity.equals(iriIndividual));

            corruptedTriple.subject = triple.subject;
            corruptedTriple.predicate = triple.predicate;
            corruptedTriple.object = corruptedEntity.getIRI().toString();
        }

        return corruptedTriple;
    }

    /**
     * Builds mapping between individuals and their classes.
     */
    private void buildIndividualsClasses() {
        individualsClasses = HashMultimap.create();
        for (OWLClass currentClass: ontology.getClassesInSignature()) {
            for (OWLNamedIndividual currentIndividual:
                    reasoner.getInstances(currentClass, false).getFlattened()) {
                individualsClasses.put(currentIndividual, currentClass);
            }
        }
    }

    /**
     * Builds mapping between classes and their individuals.
     */
    private void buildClassesIndividuals() {
        classesIndividuals = HashMultimap.create();
        for (OWLClass owlClass: ontology.getClassesInSignature()) {
            classesIndividuals.putAll(owlClass, reasoner.getInstances(owlClass, false).getFlattened());
        }
    }
}
