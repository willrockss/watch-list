const http = require('http');
const fs = require('fs');
const fsProm = require('fs/promises');
const Buffer = require('buffer').Buffer;
const url = require('url');
const progress = require('progress-stream');
const streamThrottle = require("stream-throttle")
const port = 8000;

async function delay(time) {
    return new Promise(resolve => setTimeout(resolve, time));
}

const requestListener = async function (req, res) {
    if (req.method !== 'GET') {
        res.writeHead(400);
        res.end(req.method + ' is not supported!');
        return;
    } else if (req.url === '/' || new String(req.url).toLowerCase === '/index.html') {
        showIndex(req, res);
        return;
    } else if (req.url === '/favicon.ico') {
        res.writeHead(404);
        res.end();
        return;
    }

    const params = parseParams(req);
    console.log('params', params);

    var file = params.path;

    var fileName = file.substring(file.lastIndexOf('/') + 1);


    const pathName = url.parse(req.url).pathname;
    const rawSeriesId = pathName.substring(pathName.lastIndexOf('/') + 1);
    const videoId = decodeURIComponent(rawSeriesId);


    var range = req.headers.range;
    console.log('receve request', req.method, ', url:', req.url, ', range:', range);

    if(!range) range = 'bytes=0-';

    var positions = range.replace(/bytes=/, "").split("-");
    var start = parseInt(positions[0], 10);

    let prevProgress = 0;
    let stats;
    try {
        stats = await fsProm.stat(file);
    } catch(err) {
        console.error('Unable to read file: ', file, err);
        res.writeHead(404);
        res.end();
        return;
    }

    var total = stats.size;
    var end = positions[1] ? parseInt(positions[1], 10) : total - 1;
    var chunksize = (end - start) + 1;

    start = Math.min(start, end);

    const respHeaders = {
        "Content-Range" : "bytes " + start + "-" + end + "/" + total,
        "Accept-Ranges" : "bytes",
        "Content-Length" : chunksize,
        "Content-Type" : 'video/x-matroska'
    };

    const etagValue = req.headers['If-Match'];
    if (etagValue) {
        respHeaders['etag'] = 'W/"' + etagValue + '"';
    }
    console.log('respHeaders', respHeaders);
    res.writeHead(206, respHeaders);

    var progressStream = progress({
        transferred: start,
        length: total,
        time: 2000 /* ms */
    });

    progressStream.on('progress', function(progress) {
        const currProgress =  progress.percentage.toFixed(2);
        if (prevProgress == 0 && (Math.abs(currProgress - 100) < 0.1)) {
            console.log('Ignore first 100% progress');
        } else {
            console.log('WatchProgress:', currProgress + '%');
            prevProgress = currProgress;
            sendProgress({
                progress: progress.percentage,
                videoId: videoId,
                videoPath: fileName,
                videoType: params.videoType
            });
        }
            
    });

    // Chuncksize matters how much Video Player (or TV) bufferize. Delta betwee loaded and watched time will be minimal.
    // TODO adopt chunksize based on movie metadata
    var throttleChunksize = 128 * 1024;
    if (params.videoType === 'MOVIE') {
        throttleChunksize = 512 * 1024;
        console.log("For movie use extended chunksize", throttleChunksize);
    }
    var throttle = new streamThrottle.Throttle({rate: 2*1024*1024  /*bytes per second*/ , chunksize: throttleChunksize});

    var stream = fs.createReadStream(file, {
        start : start,
        end : end
    }).on("open", function() {
        stream
            .pipe(throttle)
            .pipe(progressStream)
            .pipe(res);
    }).on("error", function(err) {
        res.end(err);
    });
};

function sendProgress(progressRequest) {
    // TODO use ZeroMQ instead of REST
    const progressReqBody = JSON.stringify(progressRequest);

    // TODO take URL from environment or parameter `progressCallback`
    const progressReq = http.request({
        host: 'localhost',
        port: 8060,
        path: '/v2/progress',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=utf-8',
            'Content-Length': Buffer.byteLength(progressReqBody, 'utf8')
        }
    });

    progressReq.on('error', (e) => {
        console.error(`problem with request: ${e.message}`);
    });

    progressReq.write(progressReqBody);
    progressReq.end();
}

const server = http.createServer(requestListener);
server.listen(port, () => {
    console.log(`Server is running on http://${process.env.CURR_HOST}:${port}`);
});


function showIndex(req, res) {
    const indexContent = 
    '<!DOCTYPE html>\n\
    <html lang="en">\n\
    <head>\n\
        <meta charset="UTF-8" />\n\
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />\n\
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />\n\
        <title>Video Streaming With Node</title>\n\
        <style>\n\
        body {\n\
            max-width: 100%;\n\
            height: 100vh;\n\
            background-color: rgb(14, 14, 14);\n\
            display: flex;\n\
            margin: auto;\n\
            align-items: center;\n\
            justify-content: center;\n\
        }\n\
        </style>\n\
    </head>\n\
    <body>\n\
        <video id="videoPlayer" width="70%" controls muted="false" preload="none">\n\
        <source src="/video" type="video/mp4" />\n\
        </video>\n\
    </body>\n\
    </html>';


    res.writeHead(200);
    res.write(indexContent);
    res.end();
}

function parseParams(req) {
    let q = req.url.split('?'), result = {};
    if(q.length >= 2 ){
        q[1].split('&').forEach((item) => {
             try {
               result[item.split('=')[0]] = decodeURIComponent(item.split('=')[1]);
             } catch(e) {
               result[item.split('=')[0]] = '';
             }
        })
    }
    return result;
}