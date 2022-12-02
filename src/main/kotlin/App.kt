import kotlinx.browser.document
import kotlinx.browser.window
import model.Game
import org.w3c.dom.HTMLElement
import view.GameScreen
import view.HomeScreen
import kotlin.js.Date
import kotlin.random.Random

interface IController {
    fun newGame()
    fun triangleClicked(idx: Int)
    fun checkerClicked(fieldIdx: Int)
    fun bearOffClicked()
    fun diceRollClicked()
}

data class Player(val name: String, val avatar_path: String, val isOpponent: Boolean)

class App() : IController {

    private lateinit var root: HTMLElement
    private lateinit var homeScreen: HomeScreen
    private lateinit var gameScreen: IGameScreen
    private var settings: Settings = loadSettings()
    private var game: Game? = null



    private fun findRoot(): HTMLElement {
        return document.getElementById("root") as HTMLElement
    }

    fun start() {
        root = findRoot()
        homeScreen = HomeScreen(this, root, settings)
    }


    override fun newGame() {
        var player1 = Player("Computer ${settings.level}", "assets/avatar.jpg", true)
        var player2 = Player("Player", "assets/avatar.jpg", false)
        val isPlayer1 = if (settings.color == PlayerSide.RANDOM) Random.nextBoolean() else settings.color == PlayerSide.PLAYER1
        if (isPlayer1) player1 = player2.also {player2 = player1}
        game = Game(if (isPlayer1) 1 else 2)
        gameScreen = GameScreen(this, root, player1, player2)
        gameScreen.initialDiceRoll(5, 6)
        window.setTimeout({gameScreen.moveChecker(6, 14)}, 5000)
    }

    override fun triangleClicked(idx: Int) {
        console.log("Triangle clicked: $idx")
    }

    override fun checkerClicked(fieldIdx: Int) {
        console.log("Checker cliekd: $fieldIdx")
    }

    override fun bearOffClicked() {
        console.log("Bear off clicked")
    }

    override fun diceRollClicked() {
        console.log("Dice roll")
    }
}