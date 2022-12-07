package view

import ANIM_DURATION_DICE
import AppScreen
import IAnimatedGameScreen
import IController
import IGameScreen
import Player
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.*
import kotlinx.html.*
import kotlinx.html.div
import kotlinx.html.js.*
import kotlinx.html.dom.create
import kotlinx.html.dom.append
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.js.Date
import ANIM_DURATION_MOVE
import ANIM_DURATION_NEW_TURN
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent


val SVG_NS = "http://www.w3.org/2000/svg"
var svgLoadEpoch = 0.0

private val MODAL_ID="modal-main"

private val STR_EQUAL_ROLL = "The rolls are equal. A re-roll is needed."
private val STR_LOST_ROLL = "You lost the roll. Your opponent goes first."
private val STR_WON_ROLL = "You won the roll. You go first."
private val STR_INITIAL_ROLL = "Initial roll"
private val STR_RE_ROLL = "Roll again"
private val STR_OK = "OK"

private val STR_NO_MOVES_TITLE = "No moves"
private val STR_NO_MOVES_BODY = "You don't have any legal moves left. :("




fun displayModalDialog(title: String, body: String, buttonText: String, callback: ((Event) -> Unit)? = null) {
    val modal = document.getElementById(MODAL_ID) as HTMLElement
    val modalTitle = document.getElementById("${MODAL_ID}-title") as HTMLElement
    val modalBody = document.getElementById("${MODAL_ID}-content") as HTMLElement
    val modalButton = document.getElementById("${MODAL_ID}-button") as HTMLButtonElement
    modalTitle.innerText = title
    modalBody.innerText = body
    modalButton.innerText = buttonText

    if (callback != null) {
        modalButton.addEventListener("click", callback)
    }
    val modalShowButton = document.getElementById("${MODAL_ID}-show") as HTMLButtonElement
    modalShowButton.click()
}

class GameScreen(app: IController, root: HTMLElement, player1: Player, player2: Player)
    : AppScreen(app, root), IGameScreen, IAnimatedGameScreen {

    private val playerIndex = if (player1.isOpponent) 2 else 1
    private val scoreBoard = ScoreBoard()
    private val playerProfile1 = PlayerProfile(player1.name, player1.avatar_path, true)
    private val playerProfile2 = PlayerProfile(player2.name, player2.avatar_path, false)
    private val gameBoard = GameBoard(app)
    private lateinit var rollDiceButton: HTMLButtonElement

    init {
        renderAll()
    }

    private fun renderAll() {
        root.clear()
        val container = document.create.div("game-container") {}
        root.appendChild(container)
        container.append {
            div("status-row") {
                playerProfile1.render(this@append)
                scoreBoard.render(this@append)
                playerProfile2.render(this@append)
            }
            div ("board-container") {
                gameBoard.render(this@append)
                div ("board-overlay") {
                    div {style="width:${gameBoard.barWidth / 2}px;flex-shrink:0;"}
                    if (playerIndex == 2) div {}
                    div {
                        button (classes="rolldice-button btn btn-primary") {
                            +"Roll dice"
                            onClickFunction = {app.diceRollClicked()}
                        }
                    }
                    if (playerIndex == 1) div {}
                    div {style="width:${gameBoard.barWidth / 2}px;flex-shrink:0;"}
                }
            }
        }
        if (svgLoadEpoch < 1e9) svgLoadEpoch = Date.now()
        rollDiceButton = document.getElementsByClassName("rolldice-button")[0] as HTMLButtonElement
    }

    override fun initialDiceRoll(result1: Int, result2: Int) {
        println("Initial dice roll: $result1 - $result2")
        rollDiceButton.hidden = true
        gameBoard.initialDiceRoll(result1, result2)
        val delay = ANIM_DURATION_DICE + 1000

        // the rolls are equal: a re-roll will be performed
        if (result1 == result2) {
            window.setTimeout({
                displayModalDialog(STR_INITIAL_ROLL, STR_EQUAL_ROLL, STR_RE_ROLL) {
                    app.animationFinished()
                }
            }, delay)
        } else {
            val biggerIdx = if (result1 > result2) 1 else 2
            val modalContent = if (biggerIdx == playerIndex) STR_WON_ROLL else STR_LOST_ROLL
            window.setTimeout({
                displayModalDialog(STR_INITIAL_ROLL, modalContent, STR_OK) {
                    app.animationFinished()
                }
            }, delay)
        }
    }

    override fun playerDiceRoll(result1: Int, result2: Int, playerIdx: Int) {
        rollDiceButton.hidden = true
        gameBoard.playerDiceRoll(result1, result2, playerIdx)
    }

    override fun moveChecker(fieldFrom: Int, fieldTo: Int) {
        gameBoard.moveChecker(fieldFrom, fieldTo)
    }

    override fun bearOffChecker(fieldFrom: Int) {
        gameBoard.bearOffChecker(fieldFrom)
    }

    override fun setPlayerScores(player1: Int, player2: Int) {
        scoreBoard.setPlayerScores(player1, player2)
    }

    override fun setPosition(fields: List<Int>, fieldPlayerIdx: List<Int>, turnOf: Int, dieNums: Pair<Int, Int>?) {
        gameBoard.setFieldCheckers(fields, fieldPlayerIdx)
        dieNums?.let {
            gameBoard.playerDiceRoll(it.first, it.second, turnOf, false)
        }
        val active = if (turnOf == 1) Pair(true, false) else Pair(false, true)
        playerProfile1.isActive = active.first
        playerProfile2.isActive = active.second
        renderAll()
    }

    override fun highlightFields(fields: List<Int>) {
        gameBoard.highlightFields(fields.toSet())
    }

    override fun highlightCheckersAt(fields: List<Int>) {
        gameBoard.highlightCheckersAt(fields.toSet())
    }

    override fun focusField(field: Int?) {
        gameBoard.focusField(field)
    }

    override fun newTurnOf(playerIdx: Int) {
        console.log("New turn of $playerIdx")
        gameBoard.dice = emptyList()
        val active = if (playerIdx == 1) Pair(true, false) else Pair(false, true)
        playerProfile1.isActive = active.first
        playerProfile2.isActive = active.second
        window.setTimeout({
            if (playerIdx == playerIndex) rollDiceButton.hidden = false
            app.animationFinished()
        }, ANIM_DURATION_NEW_TURN)
    }

    override fun roundEnded() {
        // if game ended too, do not do anything
        TODO("Not yet implemented")
    }

    override fun gameEnded() {
        TODO("Not yet implemented")
    }

    override fun noMovesAvailable() {
        displayModalDialog(STR_NO_MOVES_TITLE, STR_NO_MOVES_BODY, STR_OK) {
            app.animationFinished()
        }
    }


}
