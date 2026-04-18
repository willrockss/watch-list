#!/bin/bash

# Build Docker image with multiple tags
# This script builds the watch-list-go-worker Docker image with two tags:
# - latest tag for the current build
# - versioned tag (0.1.0) for version tracking

docker build -t io.kluev/watch-list-go-worker:latest -t io.kluev/watch-list-go-worker:0.1.0 .

# Check if the build was successful
if [ $? -eq 0 ]; then
    echo "Docker image built successfully!"
    echo "Tags: io.kluev/watch-list-go-worker:latest, io.kluev/watch-list-go-worker:0.1.0"
else
    echo "Docker build failed!"
    exit 1
fi