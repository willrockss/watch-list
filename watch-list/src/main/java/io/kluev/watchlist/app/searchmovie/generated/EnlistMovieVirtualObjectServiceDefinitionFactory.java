package io.kluev.watchlist.app.searchmovie.generated;

/** Service definition factory to bind {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
public class EnlistMovieVirtualObjectServiceDefinitionFactory implements dev.restate.sdk.endpoint.definition.ServiceDefinitionFactory<io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject> {

    @Override
    public dev.restate.sdk.endpoint.definition.ServiceDefinition create(io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject bindableService, dev.restate.sdk.endpoint.definition.HandlerRunner.Options overrideHandlerOptions) {
        dev.restate.sdk.HandlerRunner.Options handlerRunnerOptions = dev.restate.sdk.HandlerRunner.Options.DEFAULT;
        if (overrideHandlerOptions != null) {
            if (overrideHandlerOptions instanceof dev.restate.sdk.HandlerRunner.Options) {
                handlerRunnerOptions = (dev.restate.sdk.HandlerRunner.Options)overrideHandlerOptions;
            } else {
                throw new IllegalArgumentException("The provided options class MUST be instance of dev.restate.sdk.HandlerRunner.Options, but was " + overrideHandlerOptions.getClass());
            }
        }
        return dev.restate.sdk.endpoint.definition.ServiceDefinition.of(
            EnlistMovieVirtualObjectHandlers.Metadata.SERVICE_NAME,
            dev.restate.sdk.endpoint.definition.ServiceType.VIRTUAL_OBJECT,
            java.util.List.of(
            
                dev.restate.sdk.endpoint.definition.HandlerDefinition.of(
                "addToWatchList",
                dev.restate.sdk.endpoint.definition.HandlerType.EXCLUSIVE,
                EnlistMovieVirtualObjectHandlers.Metadata.Serde.ADDTOWATCHLIST_INPUT,
                EnlistMovieVirtualObjectHandlers.Metadata.Serde.ADDTOWATCHLIST_OUTPUT,
                dev.restate.sdk.HandlerRunner.of(bindableService::addToWatchList, EnlistMovieVirtualObjectHandlers.Metadata.SERDE_FACTORY, handlerRunnerOptions)
                )
            
            )
        );
    }

    @Override
    public boolean supports(Object serviceObject) {
        return serviceObject instanceof io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject;
    }
}