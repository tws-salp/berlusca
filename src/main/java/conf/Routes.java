package conf;


import controllers.ApplicationController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

/**
 * Class which binds URI paths to methods.
 */
public class Routes implements ApplicationRoutes {

    /**
     * Initializes bindings to execute methods associated to specified URI paths.
     *
     * @param router HTTP request manager
     */
    @Override
    public void init(Router router) {
        router.GET().route("/").with(ApplicationController.class, "index");
        router.POST().route("/corrupted").with(ApplicationController.class, "corrupted");
        router.GET().route("/entity/id").with(ApplicationController.class, "entityURI2id");
        router.GET().route("/entity/uri").with(ApplicationController.class, "entityId2URI");
        router.GET().route("/relation/id").with(ApplicationController.class, "relationURI2id");
        router.GET().route("/relation/uri").with(ApplicationController.class, "relationId2URI");

        ///////////////////////////////////////////////////////////////////////
        // Assets (pictures / javascript)
        ///////////////////////////////////////////////////////////////////////
        router.GET().route("/assets/webjars/{fileName: .*}").with(AssetsController.class, "serveWebJars");
        router.GET().route("/assets/{fileName: .*}").with(AssetsController.class, "serveStatic");

        ///////////////////////////////////////////////////////////////////////
        // Index / Catchall shows index page
        ///////////////////////////////////////////////////////////////////////
        router.GET().route("/.*").with(ApplicationController.class, "index");
    }
}
