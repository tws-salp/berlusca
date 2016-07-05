package conf;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import models.TripleCorrupter;
import models.TripleIndexer;

/**
 * Class automatically called by Ninja Framework to configure dependency injection.
 */
@Singleton
public class Module extends AbstractModule {
    /**
     * Binds providers to classes.
     */
    protected void configure() {
        bind(TripleIndexer.class).toProvider(TripleIndexerProvider.class);
        bind(TripleCorrupter.class).toProvider(TripleCorrupterProvider.class);
    }
}
