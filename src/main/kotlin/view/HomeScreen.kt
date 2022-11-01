package view

import AppScreen
import IController
import kotlinx.browser.document
import kotlinx.dom.clear
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import Settings
import kotlinx.html.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

private fun inputFeedbackCallback(inputId: String, feedbackId: String): (Event) -> Unit {
    """Create a callback for updating an element when an input changes."""
    fun updateNumGames(event: Event) {
        val numFeedback = document.getElementById(feedbackId)
        val newval = (document.getElementById(inputId) as HTMLInputElement?)?.value
        if (newval != null) numFeedback?.innerHTML = newval
    }
    return ::updateNumGames
}

private fun HTMLElement.inputSlider(
    id_: String, label_: String, min_: String, max_: String, step_: String,
    value_: String
) {
    append.div {
        label("form-label") {
            htmlFor = id_
            +label_
        }
        span {
            id = id_ + "Feedback"
            +value_
        }
        input(InputType.range, classes = "form-range") {
            min = min_
            max = max_
            step = step_
            value = value_
            id = id_
            onChangeFunction = inputFeedbackCallback(id_, id_ + "Feedback")
        }
    }
}

private fun HTMLElement.settingsForm(settings: Settings) {
    """A group of elements containing game settings"""
    append {
        inputSlider("numGamesInput", "Number of games:", "1", "11", "2", "${settings.numGames}")
        inputSlider("levelInput", "Level:", "1", "5", "1", "${settings.level}")
    }
}

class HomeScreen(app: IController, root: HTMLElement, private val settings: Settings): AppScreen(app, root) {

    private val container: HTMLElement

    init {
        root.clear()
        container = document.create.div("container") {}
        root.appendChild(container)
        container.append {
            h1 {
                +"Backgammon"
            }
            container.settingsForm(settings)
            button(classes = "btn btn-primary") {
                onClickFunction = {
                    settings.numGames = (document.getElementById("numGamesInput") as HTMLInputElement).value.toInt()
                    settings.level = (document.getElementById("levelInput") as HTMLInputElement).value.toInt()
                    app.newGame()
                }
                +"Start"
            }
        }
    }
}

