package io.kluev.watchlist.app.searchmovie.generated;

/** Handler request factories for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
@SuppressWarnings("unchecked")
public final class EnlistMovieVirtualObjectHandlers {

    private EnlistMovieVirtualObjectHandlers() {}

    
    /**
     * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
     **/
    public static dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(String key, io.kluev.watchlist.app.EnlistMovieRequest req) {
    return  dev.restate.common.Request.of(
            dev.restate.common.Target.virtualObject(Metadata.SERVICE_NAME, key, "addToWatchList"),
            Metadata.Serde.ADDTOWATCHLIST_INPUT,
            Metadata.Serde.ADDTOWATCHLIST_OUTPUT,
            req);
    }

    

    /** Metadata for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public final static class Metadata {

        public static final String SERVICE_NAME = "EnlistMovieVirtualObject";
        public static final dev.restate.serde.SerdeFactory SERDE_FACTORY = new dev.restate.serde.jackson.JacksonSerdeFactory();

        private Metadata() {}

        public final static class Serde {
            
                public static final dev.restate.serde.Serde<io.kluev.watchlist.app.EnlistMovieRequest> ADDTOWATCHLIST_INPUT = SERDE_FACTORY.create(dev.restate.serde.TypeTag.of(new dev.restate.serde.TypeRef<io.kluev.watchlist.app.EnlistMovieRequest>() {}));
                public static final dev.restate.serde.Serde<io.kluev.watchlist.app.EnlistMovieResponse> ADDTOWATCHLIST_OUTPUT = SERDE_FACTORY.create(dev.restate.serde.TypeTag.of(new dev.restate.serde.TypeRef<io.kluev.watchlist.app.EnlistMovieResponse>() {}));
            

            private Serde() {}
        }

    }
}