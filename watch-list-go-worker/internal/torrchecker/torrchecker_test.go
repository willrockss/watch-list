package torrchecker

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"
)

func TestGetFileName(t *testing.T) {
	_, filename, _, _ := runtime.Caller(0)
	testdataPath := filepath.Join(filepath.Dir(filename), "testdata")

	tests := []struct {
		name         string
		torrentFile  string
		expectedName string
	}{
		{
			name:         "Single file torrent",
			torrentFile:  "single-video-file.torrent",
			expectedName: "MOV_1325.mp4",
		},
		{
			name:         "Multi-file torrent with single video",
			torrentFile:  "single-video-file-folder.torrent",
			expectedName: "single-video-file-folder/MOV_1324.mp4",
		},
		{
			name:         "BlueRay with single stream",
			torrentFile:  "blue-ray-single-file.torrent",
			expectedName: "MOV_1324/BDMV/STREAM/00000.m2ts",
		},
	}

	t.Parallel()
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			torrentPath := filepath.Join(testdataPath, tt.torrentFile)

			// Check if file exists
			if _, err := os.Stat(torrentPath); os.IsNotExist(err) {
				t.Fatalf("Test file does not exist: %s", torrentPath)
			}

			name, err := GetFileName(torrentPath)

			if err != nil {
				t.Errorf("Expected no error, but got: %v", err)
			}
			if name == "" {
				t.Error("Expected file name, got empty string")
			}
			if name != tt.expectedName {
				t.Errorf("Expected %s, got %s", tt.expectedName, name)
			}
		})
	}
}

func TestMultipleVideoFileError(t *testing.T) {
	_, filename, _, _ := runtime.Caller(0)
	testdataPath := filepath.Join(filepath.Dir(filename), "testdata")
	torrentPath := filepath.Join(testdataPath, "multi-video-file-folder.torrent")

	// Check if file exists
	if _, err := os.Stat(torrentPath); os.IsNotExist(err) {
		t.Fatalf("Test file does not exist: %s", torrentPath)
	}

	name, err := GetFileName(torrentPath)

	if err == nil {
		t.Fatal("Expected an error, but got nil")
	}

	// Check that the error is of type *MultipleVideoFileError
	multipleVideoErr, ok := err.(*MultipleVideoFileError)
	if !ok {
		t.Fatalf("Expected error of type *MultipleVideoFileError, but got %T", err)
	}

	// Check that the error contains a list of two video files
	expectedFiles := []string{
		"multi-file-folder/MOV_1325.mp4",
		"multi-file-folder/MOV_1324.mp4",
	}
	if len(multipleVideoErr.Files) != len(expectedFiles) {
		t.Fatalf("Expected %d files in error, but got %d", len(expectedFiles), len(multipleVideoErr.Files))
	}
	for i, expected := range expectedFiles {
		if multipleVideoErr.Files[i] != expected {
			t.Errorf("Expected file %s at index %d, got %s", expected, i, multipleVideoErr.Files[i])
		}
	}

	// Check that the name is empty
	if name != "" {
		t.Errorf("Expected empty name, got %s", name)
	}
}

func TestNoVideoFileError(t *testing.T) {
	_, filename, _, _ := runtime.Caller(0)
	testdataPath := filepath.Join(filepath.Dir(filename), "testdata")
	torrentPath := filepath.Join(testdataPath, "multi-non-video-file-folder.torrent")

	// Check if file exists
	if _, err := os.Stat(torrentPath); os.IsNotExist(err) {
		t.Fatalf("Test file does not exist: %s", torrentPath)
	}

	name, err := GetFileName(torrentPath)

	if err == nil {
		t.Fatal("Expected an error, but got nil")
	}

	// Check that the error is of type *NoVideoFileError
	_, ok := err.(*NoVideoFileError)
	if !ok {
		t.Fatalf("Expected error of type *NoVideoFileError, but got %T", err)
	}

	// Check that the name is empty
	if name != "" {
		t.Errorf("Expected empty name, got %s", name)
	}
}
