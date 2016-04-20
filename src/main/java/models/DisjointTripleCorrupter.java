package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DisjointTripleCorrupter extends TripleCorrupter {
    // Maps individuals to most specific classes
    private Multimap<OWLNamedIndividual, OWLClass> individualsClasses;

    // Maps classes to individuals
    private Multimap<OWLClass, OWLNamedIndividual> classesIndividuals;

    // Maps classes to disjoint classes
    private Multimap<OWLClass, OWLClass> disjointClasses;

    DisjointTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException {
        super(ontologyFile);

        logger.info("-- Building individuals to classes index");
        buildIndividualsClasses();

        logger.info("-- Building classes to individuals index");
        buildClassesIndividuals();

        logger.info("-- Building disjoint classes index");
        buildDisjointClasses();
    }

    @Override
    public List<Triple> corrupt(Triple triple, EntityType entityType, int size) {
        List<Triple> triples = new ArrayList<>();
        OWLNamedIndividual iriIndividual = (entityType.equals(EntityType.SUBJECT)) ?
                new OWLNamedIndividualImpl(IRI.create(triple.subject)) :
                new OWLNamedIndividualImpl(IRI.create(triple.object));

        Collection<OWLClass> iriClasses = individualsClasses.get(iriIndividual);

        if (iriClasses != null) {
            Set<OWLNamedIndividual> notIriIndividuals = new HashSet<>();
            for(OWLClass iriClass: iriClasses) {
                Collection<OWLClass> notIriClasses = disjointClasses.get(iriClass);

                for (OWLClass currentClass : notIriClasses) {
                    classesIndividuals.get(currentClass).forEach(notIriIndividuals::add);
                }
            }

            notIriIndividuals.stream().
                    limit(size).
                    forEach(i -> {
                        Triple t = new Triple();
                        if (entityType.equals(EntityType.SUBJECT)) {
                            t.subject = i.getIRI().toString();
                            t.predicate = triple.predicate;
                            t.object = triple.object;
                        } else {
                            t.subject = triple.subject;
                            t.predicate = triple.predicate;
                            t.object = i.getIRI().toString();
                        }
                        triples.add(t);
                    });
        }

        return triples;
    }

    private void buildIndividualsClasses() {
        individualsClasses = HashMultimap.create();
        for (OWLClass currentClass: ontology.getClassesInSignature()) {
            for (OWLNamedIndividual currentIndividual:
                    reasoner.getInstances(currentClass, false).getFlattened()) {
                individualsClasses.put(currentIndividual, currentClass);
            }
        }
    }

    private void buildClassesIndividuals() {
        classesIndividuals = HashMultimap.create();
        for (OWLClass owlClass: ontology.getClassesInSignature()) {
            classesIndividuals.putAll(owlClass, reasoner.getInstances(owlClass, false).getFlattened());
        }
    }

    private void buildDisjointClasses() {
        disjointClasses = HashMultimap.create();
        for (OWLClass currentClass: ontology.getClassesInSignature()) {
            disjointClasses.putAll(currentClass, ontology.classesInSignature().
                    filter(c -> !reasoner.getSuperClasses(currentClass).getFlattened().contains(c) && !currentClass.equals(c)).
                    collect(Collectors.toList()));
        }
    }

    public static void main(String [] args) throws OWLOntologyCreationException {
        TripleCorrupter tripleCorrupter = new DisjointTripleCorrupter(new File("filtered_wn31.owl"));

        Triple triple = new Triple();
        triple.subject = "http://lemon-model.net/lexica/uby/wn/WN_Sense_100036";
        triple.predicate = "https://www.w3.org/2002/07/owl/sameAs";
        triple.object = "http://wordnet-rdf.princeton.edu/wn31/blood-red-s#1-s";

        System.out.println("-- Corrupting triples");
        long startTime = System.nanoTime();

        System.out.println(tripleCorrupter.corrupt(triple, EntityType.SUBJECT, 10));

        long endTime = System.nanoTime();
        double difference = (endTime - startTime) / 1e9;
        System.out.println("-- Time: " + difference);
    }
}
