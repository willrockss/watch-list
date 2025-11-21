import threading
import time

import requests
import xbmc
import xbmcaddon
import xbmcgui

# Constants
ADDON = xbmcaddon.Addon()


class PlayerMonitor(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)
        self.player = xbmc.Player()
        self._stop_event = threading.Event()
        self.last_time = 0

    def run(self):
        xbmc.log("@@@>>> Player monitor started", level=xbmc.LOGINFO)
        while not self._stop_event.is_set():
            if self.player.isPlaying():
                self.send_progress()
                time.sleep(1)
            else:
                xbmc.log("@@@>>> Player is not playing. Sleep(5)", level=xbmc.LOGDEBUG)
                time.sleep(5)
        xbmc.log("@@@>>> Player monitor stopped", level=xbmc.LOGINFO)

    def stop(self):
        self._stop_event.set()

    def show_notification(self, heading, message, icon="info", time=5000):
        dialog = xbmcgui.Dialog()
        dialog.notification(heading, message, icon, time)

    def send_progress(self):
        current_time_sec = self.player.getTime()

        if current_time_sec <= self.last_time:
            return

        total_time_sec = self.player.getTotalTime()

        if total_time_sec > 0:
            if total_time_sec - current_time_sec < 10:
                progress_to_send_percent = 100.0
            else:
                progress_to_send_percent = current_time_sec / total_time_sec * 100
        else:
            progress_to_send_percent = 0

        li = self.player.getPlayingItem()
        video_id = li.getProperty("videoId")
        video_type = li.getProperty("videoType")
        local_path = li.getProperty("localPath")
        self.last_time = current_time_sec

        xbmc.log(
            f"@@@>>> Progress: {current_time_sec}s / {total_time_sec}s in video {self.player.getPlayingItem().getProperty('videoId')}",
            xbmc.LOGDEBUG,
        )

        try:
            watch_list_base_url = ADDON.getSettingString("watch_list_base_url")
            # TODO add url checks
            xbmc.log(f"Base URL: {watch_list_base_url}", level=xbmc.LOGINFO)
            url = watch_list_base_url + "/v3/progress"
            response = requests.post(
                url,
                json={
                    "videoId": video_id,
                    "videoType": video_type,
                    "videoPath": local_path,
                    "progress": progress_to_send_percent,
                },
                headers={"Content-Type": "application/json"},
                timeout=10,
            )
            xbmc.log(f"@@@>>> Progress sent: {response.text}", level=xbmc.LOGDEBUG)

            if response.status_code == 200:
                responseBody = response.json()
                if responseBody.get("isMarkedAsWatched"):
                    xbmc.log(
                        f"@@@>>> Video {video_id} marked as watched", level=xbmc.LOGINFO
                    )
                    self.show_notification(
                        "Watched", "Video " + local_path + " marked as watched"
                    )

        except Exception as e:
            xbmc.log(f"Error during sending progress: {str(e)}", xbmc.LOGERROR)
