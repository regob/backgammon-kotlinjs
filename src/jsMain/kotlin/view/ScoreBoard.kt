package view

import Component
import Renderable
import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.id
import org.w3c.dom.HTMLElement

class ScoreBoard : Component(), Renderable {

    override fun render(tc: TagConsumer<HTMLElement>) {
        root = tc.div {
            id = "scoreboard"
            +"0 - 0"
        }
    }

    fun setPlayerScores(score1: Int, score2: Int) {
        root?.innerHTML = "$score1 - $score2"
    }
}