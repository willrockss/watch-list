sys.path.append('/storage/.kodi/addons/script.module.requests/lib/')
sys.path.append('/storage/.kodi/addons/script.module.urllib3/lib/')

import xbmc
import xbmcgui
import xbmcplugin
import xbmcaddon
import sys
import urllib.parse
import requests

# Constants
ADDON = xbmcaddon.Addon()
HANDLE = int(sys.argv[1])

def fetch():
    """Получает список фильмов через HTTP GET"""
    try:
        watch_list_base_url = ADDON.getSettingString("watch_list_base_url")
        # TODO add url checks
        xbmc.log(f"Base URL: {watch_list_base_url}", level=xbmc.LOGINFO)
        url = watch_list_base_url + "/v2/watch-list"
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        xbmc.log(f"Ошибка: {str(e)}", xbmc.LOGERROR)
        return []

def list_videos():
    """Отображает список фильмов в Kodi"""
    response = fetch()
    episodes = response["series"]
    movies = response["movies"]

    if (is_empty(episodes, movies)):
        xbmc.log("Watch list is empty", level=xbmc.LOGINFO)
        return

    last_item = print_result(episodes)
    last_item = get_second_if_present(last_item, print_result(movies))
     
    xbmcplugin.endOfDirectory(HANDLE)
    xbmcplugin.setResolvedUrl(HANDLE, True, last_item)

def print_result(videoElements):
    for video in videoElements:
        # Create element
        title = video["title"]
        li = xbmcgui.ListItem(label=title)
        streamUrl = urllib.parse.quote(video["contentStreamUrl"])
        li.setInfo("video", {"plot": streamUrl})
        li.setArt({"poster": video.get("poster_url", "")})
        li.setProperty('IsPlayable', 'true')
        xbmc.log(f"loaded item {title} with url {streamUrl}", level=xbmc.LOGINFO)
        # Create URL to play video
        url = f"plugin://{ADDON.getAddonInfo('id')}/?video_url={streamUrl}"
        is_folder = False
        
        # Add video element into list
        xbmcplugin.addDirectoryItem(HANDLE, url, li, is_folder)
    return li


def play_movie(video_url):
    # Decode URL first to handle any double-encoding
    decoded_url = video_url

    xbmc.log(f"going to play {decoded_url}", level=xbmc.LOGINFO)
    
    # Create playable item
    li = xbmcgui.ListItem(path=decoded_url)
    
    # Set content type headers if needed
    if "m4v" in decoded_url:
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
        play_movie(params["video_url"])
    else:
        xbmc.log(f"@@@>>> streamUrl is not specified. Do nothing", level=xbmc.LOGINFO)


def is_empty(episodes, movies):
    return ((episodes is None) or (len(episodes) == 0)) and ((movies is None) or (len(movies) == 0))

def get_second_if_present(first, second):
    return second if second is not None else first

if __name__ == "__main__":
    router(sys.argv[2][1:])
