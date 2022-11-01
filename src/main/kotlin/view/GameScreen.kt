package view

import ActivateableComponent
import AppScreen
import Component
import IController
import Player
import Renderable
import RenderableInto
import kotlinx.browser.document
import kotlinx.dom.*
import kotlinx.html.*
import kotlinx.html.div
import kotlinx.html.js.*
import kotlinx.html.dom.create
import kotlinx.html.dom.append
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.svg.SVGElement
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

private val SVG_NS = "http://www.w3.org/2000/svg"

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

class ScoreBoard() : Component(), Renderable {

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


class Triangle(
    val baseCenter: Pair<Int, Int>, val height: Int, val width: Int,
    private val isDark: Boolean
): ActivateableComponent(), RenderableInto {

    var listener: (Event) -> Unit = {}
        set (value) {
            field = value
            if (root != null) (root as SVGElement).onclick = value
        }

    override fun render(parent: HTMLElement) {
        val elem = document.createElementNS(SVG_NS, "polygon") as SVGElement
        elem.classList.add(if (isDark) "dark" else "light")
        val (x, y) = baseCenter
        val (x1, x2) = Pair(max(0, x - width / 2), x + width / 2)
        val yTop = y + height
        elem.setAttribute("points", "$x1,$y $x2,$y, $x,$yTop")
        parent.appendChild(elem)
        elem.onclick = listener
        root = elem
    }
}

class Checker(
    private val center: Pair<Int, Int>, private val radius: Int, private val isDark: Boolean
): ActivateableComponent(), RenderableInto {
    var listener: (Event) -> Unit = {}
        set (value) {
            field = value
            if (root != null) (root as SVGElement).onclick = value
        }

    override fun render(parent: HTMLElement) {
        val elem = document.createElementNS(SVG_NS, "circle") as SVGElement
        elem.classList.add(if (isDark) "dark" else "light")
        elem.setAttribute("cx", "${center.first}")
        elem.setAttribute("cy", "${center.second}")
        elem.setAttribute("r", "$radius")
        parent.append(elem)
        elem.onclick = listener
        root = elem
    }
}

class Die (
    private val number: Int, private val center: Pair<Int, Int>, private val size: Int
): Component(), RenderableInto {
    override fun render(parent: HTMLElement) {
        val elem = document.createElementNS(SVG_NS, "image")
        elem.setAttribute("x", "${center.first - size/2}")
        elem.setAttribute("y", "${center.second - size/2}")
        elem.setAttribute("width", "$size")
        elem.setAttribute("height", "$size")
        elem.setAttribute("href", "die_${number}.svg")
        parent.append(elem)
        root = elem
    }
}


class GameBoard(private val app: IController) : Component(), Renderable {

    private val svgWidth = 720
    private val svgHeight = 400
    private val borderWidth = 20
    private val barWidth = 36
    private val barAreaHeight = (svgHeight - 2 * borderWidth) * 2 / 3
    private val triangleHeight = 150
    private val triangleWidth = (svgWidth - 3 * barWidth) / 12
    private val checkerRadius = triangleHeight / 10

    private val triangles: List<Triangle> = initTriangles()
    private val checkersAt: List<List<Checker>> = initCheckers()

    private fun initTriangles(): List<Triangle> {
        val l = mutableListOf<Triangle>()

        fun getTriangleX(i: Int): Int {
            var x = triangleWidth / 2 + (12 - i) * triangleWidth + barWidth
            if (i <= 6) x += barWidth
            return x
        }
        for (i in 1..12) {
            val x = getTriangleX(i)
            l.add(Triangle(Pair(x, svgHeight - borderWidth), -triangleHeight, triangleWidth, i % 2 == 0))
        }
        for (i in 13..24) {
            val x = getTriangleX(24 - i + 1)
            l.add(Triangle(Pair(x, borderWidth), triangleHeight, triangleWidth, i % 2 == 0))
        }
        return l
    }

    private fun calcCheckerPosition(field: Int, numCheckers: Int, checkerIdx: Int): Pair<Int, Int> {
        // the place for checkers spans from y1 to y2 along axis x
        var (x, y1, y2) = listOf(0, 0, 0)

        // on player1's bar
        if (field == 0) {
            x = svgWidth / 2
            y1 = svgHeight / 2 + checkerRadius
            y2 = svgHeight / 2 + barAreaHeight
        } else if (field == 25) { // on player2's bar
            x = svgWidth / 2
            y1 = svgHeight / 2 - checkerRadius
            y2 = svgHeight / 2 - barAreaHeight
        } else { // on triangles 1-24
            val t = triangles[field - 1]
            x = t.baseCenter.first
            y1 = t.baseCenter.second
            y2 = y1 + t.height
        }
        val sign = (y2 - y1).sign

        // if the checkers can be drawn completely
        if (abs(y2 - y1) >= numCheckers * checkerRadius * 2) {
            return Pair(x, y1 + sign * checkerRadius * (checkerIdx * 2 - 1))
        }

        // if not, divide them evenly, overlapping
        val step = ((abs(y2 - y1).toDouble() - 2 * checkerRadius) / (numCheckers - 1)).roundToInt()
        return Pair(x, y1 + sign * (checkerRadius + step * (checkerIdx - 1)))
    }

    private fun initCheckers(): List<List<Checker>> {
        val l = List<MutableList<Checker>>(26) {mutableListOf()}
        val initialPlayer1 = listOf(Pair(1, 2), Pair(12, 5), Pair(17, 3), Pair(19, 5))
        for ((field, cnt) in initialPlayer1) {
            for (i in 1..cnt) {
                val center = calcCheckerPosition(field, cnt, i)
                l[field].add(Checker(center, checkerRadius, true))
            }
            for (i in 1..cnt) {
                val center = calcCheckerPosition(24 - field + 1, cnt, i)
                l[24 - field + 1].add(Checker(center, checkerRadius, false))
            }
        }
        return l
    }

    override fun render(tc: TagConsumer<HTMLElement>) {
        val board = tc.svg("board") {}
        board.setAttribute("style", "width:${svgWidth}px;height:${svgHeight}px")

        // render background first
        val backgr = document.createElementNS(SVG_NS, "rect") as SVGElement
        backgr.setAttribute("x", "0")
        backgr.setAttribute("y", "0")
        backgr.setAttribute("width", "$svgWidth")
        backgr.setAttribute("height", "$svgHeight")
        backgr.classList.add("background")
        board.appendChild(backgr)

        // inner background (in board)
        val ibackgr = document.createElementNS(SVG_NS, "rect") as SVGElement
        ibackgr.setAttribute("x", "$barWidth")
        ibackgr.setAttribute("y", "$borderWidth")
        ibackgr.setAttribute("width", "${svgWidth - 2 * barWidth}")
        ibackgr.setAttribute("height", "${svgHeight - 2 * borderWidth}")
        ibackgr.classList.add("board-background")
        board.appendChild(ibackgr)


        for (t in triangles) t.render(board)
        for ((i, t) in triangles.withIndex()) {
            t.listener = {app.triangleClicked(i + 1)}
        }
        for ((i, checkers) in checkersAt.withIndex()) {
            for (ch in checkers) {
                ch.render(board)
                ch.listener = {app.checkerClicked(i)}
            }
        }

        // render the bars
        val barX = listOf(0, (svgWidth - barWidth) / 2, svgWidth - barWidth)
        val barY = listOf(borderWidth, borderWidth - 1, borderWidth)
        val barWidth = List(3) {barWidth}
        val barHeight = MutableList(3) {svgHeight - 2 * borderWidth}
        barHeight[1] += 2 // white line rendering issue (one pixel overlap needed)

        for (i in 0..2) {
            val bar = document.createElementNS(SVG_NS, "rect") as SVGElement
            bar.setAttribute("x", barX[i].toString())
            bar.setAttribute("y", barY[i].toString())
            bar.setAttribute("width", barWidth[i].toString())
            bar.setAttribute("height", barHeight[i].toString())
            bar.classList.add("bar")
            board.appendChild(bar)
            if (i == 2) bar.onclick = {app.bearOffClicked()}
        }

        // try a die
        val die = Die(1, Pair(200, 200), 20)
        die.render(board)

        root = board
    }

    fun moveChecker(fieldFrom: Int, fieldTo: Int) {

    }
}

class GameScreen(app: IController, root: HTMLElement, player1: Player, player2: Player) : AppScreen(app, root) {

    private val scoreBoard = ScoreBoard()
    private val playerProfile1 = PlayerProfile(player1.name, player1.avatar_path, true)
    private val playerProfile2 = PlayerProfile(player2.name, player2.avatar_path, false)
    private val gameBoard = GameBoard(app)

    init {
        root.clear()
        val container = document.create.div("game-container") {}
        root.appendChild(container)
        container.append {
            div("status-row") {
                playerProfile1.render(this@append)
                scoreBoard.render(this@append)
                playerProfile2.render(this@append)
            }
            gameBoard.render(this@append)
        }
    }
}
