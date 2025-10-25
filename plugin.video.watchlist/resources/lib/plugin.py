sys.path.append('/storage/.kodi/addons/script.module.requests/lib/')
sys.path.append('/storage/.kodi/addons/script.module.urllib3/lib/')

import xbmc
import xbmcgui
import xbmcplugin
import xbmcaddon
import sys
import urllib.parse
import requests

import monitor

# Constants
ADDON = xbmcaddon.Addon()
HANDLE = int(sys.argv[1])

def fetch():
    try:
        watch_list_base_url = ADDON.getSettingString("watch_list_base_url")
        # TODO add url checks
        xbmc.log(f"Base URL: {watch_list_base_url}", level=xbmc.LOGINFO)
        url = watch_list_base_url + "/v2/watch-list"
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        data = response.json()
        
        # Ensure we always return a dictionary with expected keys
        if isinstance(data, dict):
            return data
        else:
            xbmc.log(f"Unexpected response format: {data}", xbmc.LOGERROR)
            return {"series": [], "movies": []}
            
    except Exception as e:
        xbmc.log(f"Error during fetching watch list: {str(e)}", xbmc.LOGERROR)
        return {"series": [], "movies": []}  # Return dict instead of list

def list_videos():
    response = fetch()
    xbmc.log(f"fetch response: {str(response)}", xbmc.LOGDEBUG)
    episodes = response["series"]
    movies = response["movies"]

    if (is_empty(episodes, movies)):
        xbmc.log("Watch list is empty", level=xbmc.LOGINFO)
        return
    
    monitor.PlayerMonitor().start()

    last_item = print_result(episodes, 'EPISODE')
    last_item = get_second_if_present(last_item, print_result(movies, 'MOVIE'))
     
    xbmcplugin.endOfDirectory(HANDLE)
    xbmcplugin.setResolvedUrl(HANDLE, True, last_item)

def print_result(videoElements, videoType):
    if not videoElements:  # Checks for None or empty list
        return None

    for video in videoElements:
        # Create element
        id = video["id"]
        title = video["title"]
        localPath = urllib.parse.quote(video["localPath"])
        streamUrl = urllib.parse.quote(video["contentStreamUrl"])
        
        li = xbmcgui.ListItem(label=title, path=streamUrl)
        li.setArt({"poster": video.get("poster_url", "")})
        li.setProperty('IsPlayable', 'true')
        xbmc.log(f"loaded item {title} with url {streamUrl}", level=xbmc.LOGINFO)
        # Create URL to play video
        url = f"plugin://{ADDON.getAddonInfo('id')}/?video_url={streamUrl}&video_id={id}&video_type={videoType}&local_path={localPath}"

        # Add video element into list
        xbmcplugin.addDirectoryItem(HANDLE, url, li)
    return li


def play_movie(video_url, video_id, video_type, local_path):
    xbmc.log(f"@@@>>> play_movie video_url={video_url} video_id={video_id} video_type={video_type} local_path={local_path}", level=xbmc.LOGINFO)

    # Create playable item
    li = xbmcgui.ListItem(path=video_url + '&playerCapabilities=progressTracker') # TODO add check if single query param
    li.setProperty('videoId', video_id)
    li.setProperty('videoType', video_type)
    li.setProperty('localPath', local_path)

    
    # Set content type headers if needed
    if "m4v" in video_url:
        li.setMimeType('video/mp4')
        li.setContentLookup(False)

    # Pass to player
    xbmcplugin.setResolvedUrl(HANDLE, True, li)

def router(paramstring):
    xbmc.log(f"@@@>>> paramstring={paramstring}", level=xbmc.LOGINFO)
    params = dict(urllib.parse.parse_qsl(paramstring))
    xbmc.log(f"@@@>>> params={params}", level=xbmc.LOGINFO)

    streamUrl = params.get("video_url", "")

    if not params:
        xbmc.log("@@@>>> No params. Just show list", level=xbmc.LOGINFO)
        list_videos()
    elif streamUrl != "":
        xbmc.log(f"@@@>>> Going to play {streamUrl}", level=xbmc.LOGINFO)
        play_movie(params["video_url"], params["video_id"], params["video_type"], params["local_path"])
    else:
        xbmc.log(f"@@@>>> streamUrl is not specified. Do nothing", level=xbmc.LOGINFO)


def is_empty(episodes, movies):
    return ((episodes is None) or (len(episodes) == 0)) and ((movies is None) or (len(movies) == 0))

def get_second_if_present(first, second):
    return second if second is not None else first

if __name__ == "__main__":
    router(sys.argv[2][1:])
