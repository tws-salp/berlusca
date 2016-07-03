package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import controllers.data.Request;
import controllers.data.Triple;
import models.TripleCorrupter;
import models.TripleIndexer;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class which defines methods used by the router to satisfy specific requests.
 */
@Singleton
public class ApplicationController {
    @Inject
    private TripleCorrupter tripleCorrupter;
    @Inject
    private TripleIndexer tripleIndexer;

    private final Logger logger = Logger.getLogger(ApplicationController.class.getName());

    /**
     * Generates a JSON structure containing a list of corrupted triples for each input triple.
     *
     * @param request Request object for the triple corrupter
     * @return JSON structure of corrupted triples
     */
    public Result corrupted(Request request) {
        logger.info("-- Received new request");

        List<List<Triple>> corruptedTriples = request.triples.stream().
                map(triple ->
                        tripleCorrupter.corrupt(
                                triple,
                                request.size, tripleIndexer)
                )
                .collect(Collectors.toList());
        return Results.json().render(corruptedTriples);
    }

    /**
     * Converts a given entity URI to its integer identifier.
     *
     * @param uri URI to be converted
     * @return Integer identifier of the given URI
     */
    public Result entityURI2id(@Param("uri") String uri) {
        logger.info("-- Looking up for entity ID with URI " + uri);
        return Results.json().render(tripleIndexer.getEntity2id().get(uri));
    }

    /**
     * Converts a given relation URI to its integer identifier.
     *
     * @param uri URI to be converted
     * @return Integer identifier of the given URI
     */
    public Result relationURI2id(@Param("uri") String uri) {
        logger.info("-- Looking up for relation ID with URI " + uri);
        return Results.json().render(tripleIndexer.getRelation2id().get(uri));
    }

    /**
     * Converts a given entity identifier to its URI.
     *
     * @param id Integer identifier to be converted
     * @return Entity URI of the given integer identifier
     */
    public Result entityId2URI(@Param("id") Long id) {
        logger.info("-- Looking up for entity URI with ID " + id);
        return Results.json().render(tripleIndexer.getId2entity().get(id));
    }

    /**
     * Converts a given relation identifier to its URI.
     *
     * @param id Integer identifier to be converted
     * @return Entity URI of the given integer identifier
     */
    public Result relationId2URI(@Param("id") Long id) {
        logger.info("-- Looking up for relation URI with ID " + id);
        return Results.json().render(tripleIndexer.getId2relation().get(id));
    }


    /**
     * Returns a default index page.
     *
     * @return Default index page
     */
    public Result index() {
        return Results.html();
    }
}
