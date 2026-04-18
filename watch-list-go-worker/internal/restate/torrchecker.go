package restate

import (
	"errors"
	"fmt"
	"io/fs"

	restate "github.com/restatedev/sdk-go"
	"io.kluev/watch-list-go-worker/internal/torrchecker"
)

// AnalysisError represents different types of errors that can occur during analysis
type AnalysisError struct {
	Type    string   `json:"type"`            // "NoVideoFileError", "MultipleVideoFileError", etc.
	Message string   `json:"message"`         // Human-readable error message
	Files   []string `json:"files,omitempty"` // Additional info for multiple files case
}

// AnalyzeResponse represents the output of analyzing a torrent file
type AnalyzeResponse struct {
	FilePath string         `json:"filePath,omitempty"` // Path to video file if found
	Error    *AnalysisError `json:"error,omitempty"`    // Error info if no video file found
}

// AnalyzeRequest holds parameters for analysis
type AnalyzeRequest struct {
	LocalFilePath string `json:"localFilePath"` // Path to the local torrent file
}

// TorrCheckerService implements the virtual object for torrent file analysis
// It encapsulates the torrchecker.GetFileName functionality as a restate service
type TorrCheckerService struct{}

// Analyze is the entry point for the virtual object, takes a torrent file path and returns the determined video file path
func (s *TorrCheckerService) Analyze(ctx restate.ObjectContext, request AnalyzeRequest) (AnalyzeResponse, error) {
	result, err := torrchecker.GetFileName(request.LocalFilePath)
	if err != nil {
		// Handle different error types appropriately
		switch typedErr := err.(type) {
		case *torrchecker.NoVideoFileError:
			return AnalyzeResponse{
				FilePath: "",
				Error: &AnalysisError{
					Type:    "NoVideoFileError",
					Message: "No video file found in torrent",
				},
			}, nil // Return nil error so restate doesn't retry
		case *torrchecker.MultipleVideoFileError:
			return AnalyzeResponse{
				FilePath: "",
				Error: &AnalysisError{
					Type:    "MultipleVideoFileError",
					Message: "Multiple video files found in torrent",
					Files:   typedErr.Files, // Include the list of files that were found
				},
			}, nil // Return nil error so restate doesn't retry
		default:
			// Check if the error is related to file system access (like os.Open errors)
			// If it is, we should return a TerminalError to prevent Restate from retrying
			// We check if the error message contains typical os.Open issues
			if errors.Is(err, fs.ErrNotExist) || errors.Is(err, fs.ErrPermission) {
				// These are file system errors - use TerminalError to prevent retries
				return AnalyzeResponse{}, restate.TerminalError(fmt.Errorf("failed to access torrent file %s: %w", request.LocalFilePath, err))
			}

			// For other unexpected errors, we want restate to retry
			return AnalyzeResponse{}, fmt.Errorf("failed to analyze torrent file %s: %w", request.LocalFilePath, err)
		}
	}

	// Success case
	return AnalyzeResponse{
		FilePath: result,
		Error:    nil,
	}, nil
}
