import xbmc

# Ждём полной загрузки Kodi
monitor = xbmc.Monitor()

# Можно подождать несколько секунд, чтобы интерфейс точно готов
if not monitor.waitForAbort(3):
    # Открываем ваш плагин в окне Видео
    xbmc.executebuiltin(
        'ActivateWindow(Videos, plugin://plugin.video.watchlist/, return)'
    )
