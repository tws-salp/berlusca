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
import controllers.data.Request;
import controllers.data.Triple;
import models.TripleCorrupter;
import models.TripleCorrupterType;
import ninja.Context;
import ninja.Result;
import ninja.Results;
import ninja.utils.NinjaProperties;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Singleton
public class ApplicationController {
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    @Inject
    private TripleCorrupter tripleCorrupter;

    public Result corrupted(Request request, Context context) {
        executorService.execute(() -> {
            List<List<Triple>> corruptedTriples = request.triples.stream().
                    map(triple -> tripleCorrupter.corrupt(triple, TripleCorrupter.EntityType.valueOf(request.entity), request.size)).
                    collect(Collectors.toList());
            context.returnResultAsync(Results.json().render(corruptedTriples));
        });

        return Results.async();
    }

    public Result index() {
        return Results.html();
    }
}
