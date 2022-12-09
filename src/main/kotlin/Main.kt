import kotlinx.browser.window

// animation parameters for each element (in ms)
val ANIM_DURATION_DICE = 1000
val ANIM_FREQ_DICE = 150
val ANIM_DURATION_MOVE = 1000
val ANIM_FREQ_MOVE = 10
val ANIM_DURATION_NEW_ROUND = 1000
val ANIM_DURATION_NEW_TURN = 400
val ANIM_DURATION_ROUND_END = 1500
val ANIM_DURATION_GAME_END = 1500



fun main() {
    val app: App = App()
    window.onload = {
        app.start()
    }
}