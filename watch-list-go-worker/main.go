package main

import (
	"context"
	"log"
	"os"

	restate "github.com/restatedev/sdk-go"
	"github.com/restatedev/sdk-go/server"
	restatehandlers "io.kluev/watch-list-go-worker/internal/restate"
)

func main() {
	service := &restatehandlers.TorrCheckerService{}
	// Use reflection to convert your struct into a service definition
	serviceDef := restate.Reflect(service)

	// Create a Restate server using NewRestate from the server module
	restateServer := server.NewRestate()
	// Bind the service
	restateServer.Bind(serviceDef)

	// Get port from environment variable, default to 9080
	port := os.Getenv("PORT")
	if port == "" {
		port = "9080"
	}
	addr := ":" + port

	log.Printf("Starting watch-list-go-worker server on %s", addr)
	ctx := context.Background()
	if err := restateServer.Start(ctx, addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
