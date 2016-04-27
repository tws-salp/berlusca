package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.NodeSet;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimilarityTripleCorrupter extends TripleCorrupter {
    private final Random randomEntityGenerator;
    private final Map<OWLClass, Map<OWLClass, Double>> nodesLCDistances;
    private Graph<OWLClass, DefaultEdge> conceptHierarchy;
    private double hierarchyDepth;
    // Maps classes to individuals
    private Multimap<OWLClass, OWLNamedIndividual> classesIndividuals;

    public SimilarityTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        super(ontologyFile);
        this.randomEntityGenerator = new Random(RANDOM_SEED);
        this.conceptHierarchy = buildConceptHierarchy();
        Map<OWLClass, Map<OWLClass, Integer>> nodeDistances = computeNodeDistances();
        this.hierarchyDepth = computeHierarchyDepth(nodeDistances);
        this.nodesLCDistances = computeLeacockChodorowDistances(nodeDistances);

        logger.info("-- Building classes to individuals index");
        buildClassesIndividuals();

    }

    private Map<OWLClass, Map<OWLClass, Double>> computeLeacockChodorowDistances(
            Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        Map<OWLClass, Map<OWLClass, Double>> lcDistances = new HashMap<>();

        for (OWLClass c1 : nodeDistances.keySet()) {
            Map<OWLClass, Integer> c1Distances = nodeDistances.get(c1);
            Map<OWLClass, Double> c1LCDistances = lcDistances.get(c1);
            if (c1LCDistances == null) {
                c1LCDistances = new HashMap<>();
                lcDistances.put(c1, c1LCDistances);
            }

            for (OWLClass c2 : c1Distances.keySet()) {
                c1LCDistances.put(c2, computeLeacockChodorow(c1, c2, nodeDistances));
            }
        }

        return lcDistances;
    }

    @Override
    protected Triple corrupt(Triple triple, boolean corruptSubject) {
        OWLNamedIndividual iriIndividual = (corruptSubject) ?
                new OWLNamedIndividualImpl(IRI.create(triple.subject)) :
                new OWLNamedIndividualImpl(IRI.create(triple.object));
        Triple corruptedTriple = null;
        OWLClass individualClass = reasoner.getTypes(iriIndividual, true).getFlattened().iterator().next(),
                mostDistantClass = null;

        Map<OWLClass, Double> individualDistances = nodesLCDistances.get(individualClass);

        for (OWLClass c : individualDistances.keySet()) {
            if (mostDistantClass != null) {
                if (individualDistances.get(mostDistantClass) < individualDistances.get(c)) {
                    mostDistantClass = c;
                }
            } else {
                mostDistantClass = c;
            }
        }

        List<OWLNamedIndividual> corruptedIndividuals = new ArrayList<>(classesIndividuals.get(mostDistantClass));

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

        return corruptedTriple;
    }


    private Map<OWLClass, Map<OWLClass, Integer>> computeNodeDistances() {
        Map<OWLClass, Map<OWLClass, Integer>> distances = new HashMap<>();
        FloydWarshallShortestPaths<OWLClass, DefaultEdge> floydAlgorithm = new FloydWarshallShortestPaths<>(conceptHierarchy);

        for (GraphPath<OWLClass, DefaultEdge> path : floydAlgorithm.getShortestPaths()) {
            OWLClass currentClass = path.getStartVertex();
            Map<OWLClass, Integer> classDistances = distances.get(currentClass);

            if (classDistances == null) {
                classDistances = new HashMap<>();
                distances.put(currentClass, classDistances);
            }

            classDistances.put(path.getEndVertex(), path.getEdgeList().size());
        }

        return distances;
    }

    private int computeHierarchyDepth(Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        OWLClass rootNode = reasoner.getTopClassNode().getRepresentativeElement();
        Map<OWLClass, Integer> rootDistances = nodeDistances.get(rootNode);

        return rootDistances.values().stream().max(Integer::compareTo).get();
    }

    // -log (length / (2 * D))
    private double computeLeacockChodorow(OWLClass a, OWLClass b, Map<OWLClass, Map<OWLClass, Integer>> nodeDistances) {
        return -Math.log10(nodeDistances.get(a).get(b) / (2 * hierarchyDepth));
    }

    private Graph<OWLClass, DefaultEdge> buildConceptHierarchy() throws IOException {
        Graph<OWLClass, DefaultEdge> conceptHierarchy =
                new SimpleDirectedGraph<>(DefaultEdge.class);
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

    public void saveConceptHierarchy(File graphFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(graphFile), StandardCharsets.UTF_8))) {
            writer.print("digraph FS {");

            for (OWLClass src : conceptHierarchy.vertexSet()) {
                for (OWLClass dst : conceptHierarchy.vertexSet()) {
                    if (conceptHierarchy.getEdge(src, dst) != null) {
                        writer.printf("\"%s\" -> \"%s\";",
                                src,
                                dst);
                        writer.append(System.getProperty("line.separator"));
                    }
                }
            }

            writer.print("}");
        }
    }

    private void buildClassesIndividuals() {
        classesIndividuals = HashMultimap.create();
        for (OWLClass owlClass : ontology.getClassesInSignature()) {
            classesIndividuals.putAll(owlClass, reasoner.getInstances(owlClass, false).getFlattened());
        }
    }


    public static void main(String[] args) throws OWLOntologyCreationException, IOException {
        SimilarityTripleCorrupter tripleCorrupter = new SimilarityTripleCorrupter(new File("wine.rdf"));
        tripleCorrupter.saveConceptHierarchy(new File("conceptHierarchy.dot"));

    }
}
