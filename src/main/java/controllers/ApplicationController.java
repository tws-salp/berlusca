/**
 * Copyright (C) 2013 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.StringMapKey;
import controllers.data.Request;
import controllers.data.Triple;
import models.TripleCorrupter;
import models.TripleIndexer;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class ApplicationController {
    @Inject
    private TripleCorrupter tripleCorrupter;
    @Inject
    private TripleIndexer tripleIndexer;

    private final Logger logger = Logger.getLogger(ApplicationController.class.getName());

    public Result corrupted(Request request) {
        logger.info("-- Received new request");

        List<List<Triple>> corruptedTriples = request.triples.stream().
                    map(triple -> {
                        try {
                            return tripleCorrupter.corrupt(
                                    triple.decode(),
                                    request.size, tripleIndexer);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).
                    collect(Collectors.toList());
        return Results.json().render(corruptedTriples);
    }

    public Result entityURI2id(@Param("uri") String uri) {
        logger.info("-- Looking up for entity ID with URI " + uri);
        return Results.json().render(tripleIndexer.getEntity2id().get(uri));
    }

    public Result relationURI2id(@Param("uri") String uri) {
        logger.info("-- Looking up for relation ID with URI " + uri);
        return Results.json().render(tripleIndexer.getRelation2id().get(uri));
    }

    public Result entityId2URI(@Param("id") Long id) {
        logger.info("-- Looking up for entity URI with ID " + id);
        return Results.json().render(tripleIndexer.getId2entity().get(id));
    }

    public Result relationId2URI(@Param("id") Long id) {
        logger.info("-- Looking up for relation URI with ID " + id);
        return Results.json().render(tripleIndexer.getId2relation().get(id));
    }


    public Result index() {
        return Results.html();
    }
}
