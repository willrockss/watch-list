// document.fullscreenElement is not supported in Chrome 38 that's why we need this hacky method.
// Using `OR` condition because of debugger tab.
function isFullscreen() {
    var vRect = this.getBoundingClientRect();
    if (this.initialSize) {
        return vRect.width !== this.initialSize.width && vRect.height !== this.initialSize.height;
    }
    return vRect.width === window.screen.width || vRect.height === window.screen.height;
}

function setupVideoFullscreenMethod(videoElement) {
    videoElement.initialSize = videoElement.getBoundingClientRect();
    videoElement.isFullscreen = isFullscreen;
}

// fullscreenchange event is not supported by LG TV Chrome 38. Emulate this manually
function installFullscreenChecker(videoElement) {
    if (document.fsChecker && document.fsChecker.id === videoElement.id) {
        console.log('fsChecker already install for this video. Do nothing');
        return;
    }

    if (document.fsChecker) {
        console.log('Clear previous checker');
        clearInterval(document.fsChecker.intervalId);
    }

    if (typeof videoElement.lastFullscreenState === 'undefined') {
        videoElement.lastFullscreenState = false;
    }
    var intId = setInterval(function() {
        var vRect = videoElement.getBoundingClientRect();
        var initialRect = videoElement.initialSize;
        var screen = window.screen;
        var currFullscreen = videoElement.isFullscreen();

        console.log('videoSize: ', vRect.width, 'x', vRect.height, 'initialVideoSize:',  initialRect.width, 'x', initialRect.height, 'screenSize: ', screen.width, 'x', screen.height, 'currFullscreen =', currFullscreen, ' lastFullscreenState', videoElement.lastFullscreenState);
        if (currFullscreen !== videoElement.lastFullscreenState) {
            videoElement.dispatchEvent(new Event('fullscreenchange'));
        }
        videoElement.lastFullscreenState = currFullscreen;
    }, 50);
    console.log('Checker interval ', intId, ' successfully initialized');
    document.fsChecker = { id: videoElement.id, intervalId: intId };
}

function setupVideo(videoElementId, audioTrack) {
    var video = document.getElementById(videoElementId);
    if (!video) {
        console.warn('video with id ' + videoElementId + ' is not defined. Do nothing');
        return;
    }
    video.loaded = false;
    setupVideoFullscreenMethod(video);

    if (audioTrack) {
        video.onloadedmetadata = function(event) {
            var audioTrackIndex = audioTrack - 1;
            if (video.audioTracks) {
                if (audioTrackIndex < video.audioTracks.length) {
                    for (var i = 0; i < video.audioTracks.length; i += 1) {
                        console.log(JSON.stringify(video.audioTracks[i]));
                        video.audioTracks[i].enabled = false;
                    }
                    video.audioTracks[audioTrackIndex].enabled = true;
                } else {
                    console.warn('audioTrackIndex ' + audioTrackIndex + ' is invalid. There are ' + video.audioTracks.length + ' tracks only');
                }
            } else {
                console.log('Bufferization is over. No audioTracks found');
            }
        }
    }

    video.lastFullscreenState = false;
    video.addEventListener('click', function() {
        installFullscreenChecker(video);

        var isFullscreen = video.isFullscreen();
        if (!isFullscreen) {
            if (video.requestFullscreen) {
                console.log('fullscreen, default');
                video.requestFullscreen(); // Стандартный метод
            } else if (video.mozRequestFullScreen) {
                console.log('mozilla fullscreen');
                /* Firefox */ video.mozRequestFullScreen();
            } else if (video.webkitEnterFullscreen) {
                console.log('chrome fullscreen');
                /* Safari/Chrome */
                video.webkitEnterFullscreen();
            } else {
               console.log('Unknown browser and fullscreen is probably not supported');
            }
        }

        console.log('video.paused =', video.paused);
        if (video.paused) {
            console.log('video is paused, going to play');
            setTimeout(function() {
                video.play();
                console.log('video.paused after play:', video.paused);
            }, 50);
        } else {
            setTimeout(function() {
                console.log('video is playing now, going to stop');
                video.pause();
            }, 50);
        }
    });

    // fullscreenchange doesn't supported by LG TV Chrome 38.
    video.addEventListener('fullscreenchange', function(e) {
        var isFullscreen = video.isFullscreen();
        console.log('video ', video.id, 'fullscreenchange triggered. isFullscreen =', isFullscreen);

        if (!isFullscreen) {
            // Если нет, то останавливаем видео
            setTimeout(function() {
                console.log('video exit fullscreen. Stop it');
                video.pause();
            }, 50);
        }
    });

    var tooltip = document.getElementById('tooltip');

    video.addEventListener('mouseenter', function() {
        tooltip.innerText = video.src;
        tooltip.style.display = 'inline'; // show
    });

    video.addEventListener('mouseleave', function() {
        tooltip.innerText = '';
        tooltip.style.display = 'none'; // hide
    });
}
