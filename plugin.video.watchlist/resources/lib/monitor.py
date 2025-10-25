
import threading
import time
import requests

import xbmc
import xbmcaddon

# Constants
ADDON = xbmcaddon.Addon()

class PlayerMonitor(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.player = xbmc.Player()
        self._stop_event = threading.Event()
        self.last_time = 0

    def run(self):
        while not self._stop_event.is_set():
            if self.player.isPlaying():
                self.send_progress()
                time.sleep(1)
            else:
                time.sleep(5)
            

    def stop(self):
        self._stop_event.set()


    def send_progress(self):
        current_time = self.player.getTime()

        if (current_time <= self.last_time):
            return

        total_time = self.player.getTotalTime()
        
        if (total_time > 0):
            progress = current_time / total_time * 100
        else:
            progress = 0    

        li = self.player.getPlayingItem()
        video_id = li.getProperty('videoId')
        video_type = li.getProperty('videoType')
        local_path = li.getProperty('localPath')
        self.last_time = current_time

        xbmc.log(f"Progress: {current_time}s / {total_time}s in video {self.player.getPlayingItem().getProperty('videoId')}", xbmc.LOGINFO)

        try:
            watch_list_base_url = ADDON.getSettingString("watch_list_base_url")
            # TODO add url checks
            xbmc.log(f"Base URL: {watch_list_base_url}", level=xbmc.LOGINFO)
            url = watch_list_base_url + "/v2/progress"
            response = requests.post(
                url,
                json={'videoId': video_id, 'videoType': video_type, 'videoPath': local_path, 'progress': progress},
                headers={'Content-Type': 'application/json'},
                timeout=10
            )
            xbmc.log(f"Progress sent: {response.text}", level=xbmc.LOGINFO)

        except Exception as e:
            xbmc.log(f"Error during sending progress: {str(e)}", xbmc.LOGERROR)
