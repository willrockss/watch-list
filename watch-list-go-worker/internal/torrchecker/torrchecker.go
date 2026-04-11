package torrchecker

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/zeebo/bencode"
)

// Torrent represents the structure of a .torrent file
type Torrent struct {
	Info struct {
		Name string `bencode:"name"`
		// For single-file torrent
		Length int64 `bencode:"length,omitempty"`
		// For multi-file torrent
		Files []struct {
			Length int64    `bencode:"length"`
			Path   []string `bencode:"path"`
		} `bencode:"files,omitempty"`
	} `bencode:"info"`
}

// Popular video extensions
var PopularVideoExtensions = []string{
	".mkv",
	".avi",
	".mp4",
	".mov",
	".wmv",
	".flv",
	".webm",
	".m4v",
	".mpg",
	".mpeg",
	".3gp",
	".m2ts",
}

// isBlueRay checks if the torrent is a "clone" from a Blue-ray disk
func isBlueRay(torrent *Torrent) bool {
	// Check for BDMV folder at any nesting level
	for _, file := range torrent.Info.Files {
		for _, part := range file.Path {
			if strings.ToUpper(part) == "BDMV" {
				return true
			}
		}
	}
	return false
}

// MultipleVideoFileError is an error returned when multiple video files are detected
type MultipleVideoFileError struct {
	Files []string
}

func (e *MultipleVideoFileError) Error() string {
	return fmt.Sprintf("multiple video files found: %v", e.Files)
}

// NoVideoFileError is an error returned when no video files are found
type NoVideoFileError struct{}

func (e *NoVideoFileError) Error() string {
	return "no video file found in STREAM folder"
}

// findStreamFile searches for .m2ts files in the STREAM folder located inside BDMV
// Returns the file path and error:
// - nil if exactly one .m2ts file is found
// - *NoVideoFileError if no video files are found
// - *MultipleVideoFileError if more than one video file is found
func findStreamFile(torrent *Torrent) (string, error) {
	var foundPaths []string

	for _, file := range torrent.Info.Files {
		// Look for BDMV and immediately after it STREAM
		for i, part := range file.Path {
			if strings.ToUpper(part) == "BDMV" && i+1 < len(file.Path) {
				if strings.ToUpper(file.Path[i+1]) == "STREAM" {
					if strings.HasSuffix(strings.ToUpper(file.Path[len(file.Path)-1]), ".M2TS") {
						// Use torrent.Info.Name as the root folder
						fullPath := filepath.Join(append([]string{torrent.Info.Name}, file.Path...)...)
						foundPaths = append(foundPaths, fullPath)
						// Break the inner loop as the file has already been added
						break
					}
				}
			}
		}
	}

	switch len(foundPaths) {
	case 0:
		return "", &NoVideoFileError{}
	case 1:
		return foundPaths[0], nil
	default:
		return "", &MultipleVideoFileError{Files: foundPaths}
	}
}

// GetFileName returns the file name from the .torrent file:
// - for single-file torrents — the file name
// - for multi-file torrents —
//   - if the torrent is a BlueRay clone, returns the path to the .m2ts file in /STREAM/
//   - otherwise returns the name of the first file in the list
//
// - if the name cannot be determined, returns an empty string and a possible error
func GetFileName(filePath string) (string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return "", err
	}
	defer file.Close()

	var torrent Torrent
	err = bencode.NewDecoder(file).Decode(&torrent)
	if err != nil {
		return "", err
	}

	// Check for list of files (multi-file torrent)
	if len(torrent.Info.Files) > 0 {
		// Check if the torrent is a Blue-ray clone
		if isBlueRay(&torrent) {
			if streamPath, err := findStreamFile(&torrent); err != nil {
				return "", err
			} else {
				return streamPath, nil
			}
		}
		// For regular multi-file torrents, check for multiple video files
		var videoFiles []string
		for _, file := range torrent.Info.Files {
			if len(file.Path) > 0 {
				fileName := file.Path[len(file.Path)-1]
				for _, ext := range PopularVideoExtensions {
					if strings.HasSuffix(strings.ToLower(fileName), strings.ToLower(ext)) {
						videoFiles = append(videoFiles, filepath.Join(append([]string{torrent.Info.Name}, file.Path...)...))
						break
					}
				}
			}
		}
		switch len(videoFiles) {
		case 0:
			return "", &NoVideoFileError{}
		case 1:
			return videoFiles[0], nil
		default:
			return "", &MultipleVideoFileError{Files: videoFiles}
		}
	}

	// Single-file torrent: use the "name" field
	return torrent.Info.Name, nil
}
