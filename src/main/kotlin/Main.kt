import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.js.input
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.span
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.Event
import kotlin.js.Date

// event que poll frequency in App
val QUEUE_POLL_INTERVAL_MS = 30

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
        val q = document.getElementById("root") as HTMLElement
        val r = document.create.div {

        }
        0
    }
}