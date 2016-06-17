package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.apache.jena.base.Sys;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.*;
import java.util.*;

public class SimilarityTripleCorrupter extends TripleCorrupter {
    private final Map<OWLClass, Map<OWLClass, Double>> nodesLCDistances;
    private Graph<OWLClass, DefaultEdge> conceptHierarchy;
    private double hierarchyDepth;

    public SimilarityTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        super(ontologyFile);
        this.conceptHierarchy = buildConceptHierarchy();
        Map<OWLClass, Map<OWLClass, Integer>> nodeDistances = computeNodeDistances();
        this.hierarchyDepth = computeHierarchyDepth(nodeDistances);
        this.nodesLCDistances = computeLeacockChodorowSimilarities(nodeDistances);
    }

    private Map<OWLClass, Map<OWLClass, Double>> computeLeacockChodorowSimilarities(
            Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        Map<OWLClass, Map<OWLClass, Double>> lcSimilarities = new HashMap<>();

        for (OWLClass c1 : nodeDistances.keySet()) {
            Map<OWLClass, Integer> c1Distances = nodeDistances.get(c1);
            Map<OWLClass, Double> c1LCSimilarities = lcSimilarities.get(c1);
            if (c1LCSimilarities == null) {
                c1LCSimilarities = new HashMap<>();
                lcSimilarities.put(c1, c1LCSimilarities);
            }

            for (OWLClass c2 : c1Distances.keySet()) {
                c1LCSimilarities.put(c2, computeLeacockChodorow(c1, c2, nodeDistances));
            }
        }

        return lcSimilarities;
    }

    @Override
    protected Triple corrupt(Triple triple, boolean corruptSubject) {
        OWLNamedIndividual iriIndividual = (corruptSubject) ?
                new OWLNamedIndividualImpl(IRI.create(triple.subject)) :
                new OWLNamedIndividualImpl(IRI.create(triple.object));
        Triple corruptedTriple = null;
        OWLClass individualClass = null,
                nearestClass = null;

        try {

            final NodeSet<OWLClass> individualTypes = reasoner.getTypes(iriIndividual, true);

            if (individualTypes != null && !individualTypes.isEmpty()) {

                for (OWLClass owlClass : individualTypes.getFlattened()) {
                    individualClass = owlClass;
                }

                Map<OWLClass, Double> individualSimilarities = nodesLCDistances.get(individualClass);

                if (individualSimilarities != null && !individualSimilarities.isEmpty()) {

                    for (OWLClass c : individualSimilarities.keySet()) {
                        if (nearestClass != null) {
                            if (individualSimilarities.get(nearestClass) > individualSimilarities.get(c)) {
                                nearestClass = c;
                            }
                        } else {
                            nearestClass = c;
                        }
                    }

                    List<OWLNamedIndividual> corruptedIndividuals = new ArrayList<>(classesIndividuals.get(nearestClass));

                    OWLNamedIndividual corruptedEntity = corruptedIndividuals.get(
                            randomEntityGenerator.nextInt(corruptedIndividuals.size()));
                    corruptedTriple = new Triple();
                    if (corruptSubject) {
                        corruptedTriple.subject = corruptedEntity.getIRI().toString();
                        corruptedTriple.predicate = triple.predicate;
                        corruptedTriple.object = triple.object;
                    } else {
                        corruptedTriple.subject = triple.subject;
                        corruptedTriple.predicate = triple.predicate;
                        corruptedTriple.object = corruptedEntity.getIRI().toString();
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Reasoner error: unable to execute getTypes!");
        }

        return (corruptedTriple == null) ? generateRandomTriple(triple, iriIndividual, corruptSubject) : corruptedTriple;
    }


    private Map<OWLClass, Map<OWLClass, Integer>> computeNodeDistances() {
        Map<OWLClass, Map<OWLClass, Integer>> distances = new HashMap<>();
        FloydWarshallShortestPaths<OWLClass, DefaultEdge> floydAlgorithm = new FloydWarshallShortestPaths<>(conceptHierarchy);

        for (GraphPath<OWLClass, DefaultEdge> path : floydAlgorithm.getShortestPaths()) {
            OWLClass startVertex = path.getStartVertex(),
                    endVertex = path.getEndVertex();
            Map<OWLClass, Integer> startDistances = distances.get(startVertex),
                    endDistances = distances.get(endVertex);

            if (startDistances == null) {
                startDistances = new HashMap<>();
                distances.put(startVertex, startDistances);
            }
            startDistances.put(path.getEndVertex(), path.getEdgeList().size());

            if (endDistances == null) {
                endDistances = new HashMap<>();
                distances.put(endVertex, endDistances);
            }
            endDistances.put(path.getStartVertex(), path.getEdgeList().size());
        }

        return distances;
    }

    private int computeHierarchyDepth(Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        OWLClass rootNode = reasoner.getTopClassNode().getRepresentativeElement();
        Map<OWLClass, Integer> rootDistances = nodeDistances.get(rootNode);

        return rootDistances.values().stream().max(Integer::compareTo).get();
    }

    // Leacock-Chodorow similarity = -log (path_length / (2 * D))
    private double computeLeacockChodorow(OWLClass a, OWLClass b, Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        return -1 * Math.log10(1 + (nodeDistances.get(a).get(b) / (2 * hierarchyDepth)));
    }

    private Graph<OWLClass, DefaultEdge> buildConceptHierarchy() throws IOException {
        Graph<OWLClass, DefaultEdge> conceptHierarchy =
                new SimpleGraph<>(DefaultEdge.class);
        Queue<OWLClass> classQueue = new LinkedList<>();
        OWLClass topNode = reasoner.getTopClassNode().getRepresentativeElement();
        classQueue.add(topNode);
        Set<OWLClass> visitedClasses = new HashSet<>();

        while (!classQueue.isEmpty()) {
            OWLClass currentClass = classQueue.poll();
            if (!currentClass.isOWLNothing()) {
                conceptHierarchy.addVertex(currentClass);
                NodeSet<OWLClass> childrenClasses = reasoner.getSubClasses(currentClass, true);
                childrenClasses.forEach(c -> {
                    OWLClass childClass = c.getRepresentativeElement();
                    if (!visitedClasses.contains(childClass)) {
                        classQueue.add(childClass);
                    }
                });

                childrenClasses.forEach(c -> {
                    OWLClass childClass = c.getRepresentativeElement();
                    if (!childClass.isOWLNothing()) {
                        conceptHierarchy.addVertex(childClass);
                        conceptHierarchy.addEdge(currentClass, childClass);
                    }
                });

                visitedClasses.add(currentClass);
            }
        }

        return conceptHierarchy;
    }
}
