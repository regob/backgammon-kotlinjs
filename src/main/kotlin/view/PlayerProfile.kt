package view

import ActivateableComponent
import Renderable
import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.js.img
import kotlinx.html.span
import org.w3c.dom.HTMLElement

private fun TagConsumer<HTMLElement>.playerAvatar(path: String) {
    img {
        src = path
    }
}

class PlayerProfile(private val name: String, private val avatar_path: String, private val sideLeft: Boolean) :
    ActivateableComponent(), Renderable {

    override fun render(tc: TagConsumer<HTMLElement>) {
        root = tc.div("player-profile") {
            if (sideLeft) tc.playerAvatar(avatar_path)
            span {
                +name
            }
            if (!sideLeft) tc.playerAvatar(avatar_path)
        }
    }
}