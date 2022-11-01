import kotlinx.browser.document
import model.Game
import org.w3c.dom.HTMLElement
import view.GameScreen
import view.HomeScreen
import kotlin.random.Random

public interface IController {
    fun newGame()
    fun triangleClicked(idx: Int)
    fun checkerClicked(fieldIdx: Int)
    fun bearOffClicked()
}

data class Player(val name: String, val avatar_path: String)


class App() : IController {

    private lateinit var root: HTMLElement
    private lateinit var screen: AppScreen
    private var settings: Settings = loadSettings()
    private lateinit var game: Game


    private fun findRoot(): HTMLElement {
        return document.getElementById("root") as HTMLElement
    }

    fun start() {
        root = findRoot()
        screen = HomeScreen(this, root, settings)
    }



    override fun newGame() {
        var player1 = Player("Computer ${settings.level}", "avatar.jpg")
        var player2 = Player("Player", "avatar.jpg")
        val isPlayer1 = if (settings.color == PlayerSide.RANDOM) Random.nextBoolean() else settings.color == PlayerSide.PLAYER1
        if (isPlayer1) player1 = player2.also {player2 = player1}
        game = Game()
        screen = GameScreen(this, root, player1, player2)
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
}