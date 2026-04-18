FROM golang:1.26-alpine AS builder

# Install git for go modules
RUN apk add --no-cache git

# Set working directory
WORKDIR /app

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o watch-list-go-worker main.go

# Use minimal alpine image for final stage
FROM alpine:latest

# Set working directory
WORKDIR /root/

# Copy the binary from builder stage
COPY --from=builder /app/watch-list-go-worker .

EXPOSE 9080

# Command to run the executable
CMD ["./watch-list-go-worker"]