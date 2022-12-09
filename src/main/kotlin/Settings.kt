import kotlinx.browser.localStorage
import kotlin.js.JSON

enum class PlayerSide {
    PLAYER1, PLAYER2, RANDOM
}

class Settings {
    var numRounds: Int = 1
    var level: Int = 1
    var color: PlayerSide = PlayerSide.RANDOM
}

fun loadSettings(): Settings {
    val loaded = localStorage.getItem("settings")
    if (loaded != null) return JSON.parse(loaded)
    return Settings()
}

fun saveSettings(settings: Settings) {
    localStorage.setItem("settings", JSON.stringify(settings))
}

