package io.kluev.watchlist.app.searchmovie.generated;

import dev.restate.sdk.CallDurableFuture;
import dev.restate.sdk.Context;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Clients for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject }
 *
 * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject
 */
public class EnlistMovieVirtualObjectClient {

    /**
     * Create context client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject }
     *
     * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject
     */
    public static ContextClient fromContext(Context ctx, String key) {
        return new ContextClient(ctx, key);
    }

    /** Create ingress client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public static IngressClient fromClient(dev.restate.client.Client client, String key) {
        return new IngressClient(client, key);
    }

    /** Create ingress client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public static IngressClient connect(String baseUri, String key) {
        return new IngressClient(dev.restate.client.Client.connect(baseUri, EnlistMovieVirtualObjectHandlers.Metadata.SERDE_FACTORY), key);
    }

    /** Create ingress client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public static IngressClient connect(String baseUri, dev.restate.client.RequestOptions requestOptions, String key) {
        return new IngressClient(dev.restate.client.Client.connect(baseUri, EnlistMovieVirtualObjectHandlers.Metadata.SERDE_FACTORY, requestOptions), key);
    }

    /** Context client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public static class ContextClient {

        private final Context ctx;
        private final String key;

        public ContextClient(Context ctx, String key) {
            this.ctx = ctx;
            this.key = key;
        }

        
        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public CallDurableFuture<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req) {
            return this.ctx.call(
                EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req)
            );
        }

        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public CallDurableFuture<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
            var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req);
            if (requestBuilderApplier != null) {
                requestBuilderApplier.accept(reqBuilder);
            }
            return this.ctx.call(reqBuilder);
        }
        

        public Send send() {
            return new Send();
        }

        public class Send {

            
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.sdk.InvocationHandle<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req) {
                return ContextClient.this.ctx.send(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(ContextClient.this.key, req)
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.sdk.InvocationHandle<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(ContextClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return ContextClient.this.ctx.send(reqBuilder);
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.sdk.InvocationHandle<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay) {
                return ContextClient.this.ctx.send(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(ContextClient.this.key, req), delay
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.sdk.InvocationHandle<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(ContextClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return ContextClient.this.ctx.send(reqBuilder, delay);
            }
            
        }
    }

    /** Ingress client for {@link io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject } **/
    public static class IngressClient {

        private final dev.restate.client.Client client;
        private final String key;

        public IngressClient(dev.restate.client.Client client, String key) {
            this.client = client;
            this.key = key;
        }

        
        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public io.kluev.watchlist.app.EnlistMovieResponse addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req) {
            return this.client.call(
                EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req)
            ).response();
        }
        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public io.kluev.watchlist.app.EnlistMovieResponse addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
            var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req);
            if (requestBuilderApplier != null) {
                requestBuilderApplier.accept(reqBuilder);
            }
            return this.client.call(reqBuilder.build()).response();
        }

        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public java.util.concurrent.CompletableFuture<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req) {
            return this.client.callAsync(
                EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req)
            ).thenApply(dev.restate.client.Response::response);
        }
        /**
         * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
         **/
        public java.util.concurrent.CompletableFuture<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
            var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(this.key, req);
            if (requestBuilderApplier != null) {
                requestBuilderApplier.accept(reqBuilder);
            }
            return this.client.callAsync(reqBuilder.build()).thenApply(dev.restate.client.Response::response);
        }
        

        public Send send() {
            return new Send();
        }

        public class Send {

            
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req) {
                return IngressClient.this.client.send(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req)
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return IngressClient.this.client.send(reqBuilder);
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay) {
                return IngressClient.this.client.send(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req), delay
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse> addToWatchList(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return IngressClient.this.client.send(reqBuilder, delay);
            }

            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public java.util.concurrent.CompletableFuture<dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse>> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req) {
                return IngressClient.this.client.sendAsync(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req)
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public java.util.concurrent.CompletableFuture<dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse>> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return IngressClient.this.client.sendAsync(reqBuilder);
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public java.util.concurrent.CompletableFuture<dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse>> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay) {
                return IngressClient.this.client.sendAsync(
                    EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req), delay
                );
            }
            /**
             * @see io.kluev.watchlist.app.addmovie.EnlistMovieVirtualObject#addToWatchList
             **/
            public java.util.concurrent.CompletableFuture<dev.restate.client.SendResponse<io.kluev.watchlist.app.EnlistMovieResponse>> addToWatchListAsync(io.kluev.watchlist.app.EnlistMovieRequest req, Duration delay, Consumer<dev.restate.common.RequestBuilder<io.kluev.watchlist.app.EnlistMovieRequest, io.kluev.watchlist.app.EnlistMovieResponse>> requestBuilderApplier) {
                var reqBuilder = EnlistMovieVirtualObjectHandlers.addToWatchList(IngressClient.this.key, req);
                if (requestBuilderApplier != null) {
                    requestBuilderApplier.accept(reqBuilder);
                }
                return IngressClient.this.client.sendAsync(reqBuilder, delay);
            }
        }
    }
}