import ai.Computer
import kotlinx.browser.document
import model.*
import org.w3c.dom.HTMLElement
import view.GameScreen
import view.HomeScreen
import kotlin.random.Random

interface IController {
    fun newGame()
    fun newRoundClicked()
    fun fieldClicked(idx: Int)
    fun checkerClicked(fieldIdx: Int)
    fun bearOffClicked()
    fun diceRollClicked()
    fun backgroundClicked()
    fun animationFinished()
}

data class Player(val name: String, val isOpponent: Boolean)

class App :IController {

    private lateinit var root: HTMLElement
    private lateinit var homeScreen: HomeScreen
    private lateinit var gameScreen: IGameScreen
    private lateinit var animGameScreen: IAnimatedGameScreen

    private var settings: Settings = loadSettings()
    private var game: Game? = null
    private lateinit var players: Pair<Player, Player>
    private var playerIdx: Int = 1
    private var turnOf: Int = 1
    private lateinit var computer: Computer

    private val queue = ArrayDeque<GameEvent>()
    private var currentEvent: GameEvent? = null
    private var userActionsBlocked = false
    private var fieldSelected: Int? = null

    fun start() {
        root = document.getElementById("root") as HTMLElement
        homeScreen = HomeScreen(this, root, settings)
    }

    override fun newGame() {
        // initialize player profiles
        val isPlayer1 = if (settings.color == PlayerSide.RANDOM) Random.nextBoolean() else settings.color == PlayerSide.PLAYER1
        var player1 = Player("Computer ${settings.level}", true)
        var player2 = Player("Player",  false)
        if (isPlayer1) player1 = player2.also {player2 = player1}
        players = player1 to player2
        playerIdx = if (isPlayer1) 1 else 2
        // initialize the game and the game screen, where it is displayed
        gameScreen = GameScreen(this, root, player1, player2)
        animGameScreen = gameScreen as IAnimatedGameScreen
        game = Game(settings.numGames, ::receiveGameEvent)
        computer = Computer(settings.level, this::handleComputerResponse)
    }

    private fun receiveGameEvent(event: GameEvent) {
        queue.addFirst(event)
        // if the user actions are blocked, we are waiting
        // for the end of an animation and cannot handle the next event
        if (queue.size == 1 && !userActionsBlocked) handleNextEvent()
    }

    /**
     * Highlights checkers that can be moved. If a field is selected already, then highlights
     * the fields where a move is possible. If `highlight` is false, all highlights are removed.
     */
    private fun highlightBoard(highlight: Boolean = true) {
        if (!highlight) {
            gameScreen.focusField(null)
            gameScreen.highlightCheckersAt(listOf())
            gameScreen.highlightFields(listOf())
            return
        }
        if (fieldSelected != null) {
            gameScreen.focusField(fieldSelected)
            gameScreen.highlightFields(game!!.possibleFieldsFrom(fieldSelected!!))
            gameScreen.highlightCheckersAt(listOf())
        } else {
            gameScreen.focusField(null)
            gameScreen.highlightFields(listOf())
            gameScreen.highlightCheckersAt(game!!.moveableFields())
        }
    }

    private fun handleNextEvent() {
        if (queue.isEmpty()) throw IllegalStateException("No next event: queue is empty")
        val event = queue.removeLast()
        currentEvent = event
        console.log("Handling event ... ${event::class.simpleName}")
        when (event) {
            is InitialDiceRollEvent -> {
                userActionsBlocked = true
                val dice = event.dice.first()
                // if multiple rolls were performed, we only display them one-by-one
                if (event.dice.size > 1) {
                    queue.addLast(InitialDiceRollEvent(event.dice.drop(1)))
                } else turnOf = if (dice.num1 > dice.num2) 1 else 2
                animGameScreen.initialDiceRoll(dice.num1, dice.num2)
            }
            is DiceRollEvent -> {
                userActionsBlocked = true
                animGameScreen.playerDiceRoll(event.dice.num1, event.dice.num2, turnOf)
            }
            is MoveEvent -> {
                fieldSelected = null
                userActionsBlocked = true
                if (event.move.to == -1) {
                    animGameScreen.bearOffChecker(event.move.from)
                } else animGameScreen.moveChecker(event.move.from, event.move.to)
            }
            is NewRoundEvent -> {
                val init = initialGameState(1)
                gameScreen.setPosition(init.fields, init.fieldPlayerIdx, turnOf, null)
                if (queue.isNotEmpty()) handleNextEvent()
            }
            is NewTurnEvent -> {
                fieldSelected = null
                userActionsBlocked = true
                turnOf = event.turnOf
                animGameScreen.newTurnOf(turnOf)
                // if it is the computer's turn we request a dice roll and the moves
                if (turnOf != playerIdx) {
                    game!!.rollDice()
                    // if at this point if the computer did not have moves, the game progressed to the next turn
                    if (game!!.turnOf() != playerIdx) computer.query(game!!.gameState!!)
                }
                highlightBoard(false)
            }
            is RoundEndEvent -> {
                gameScreen.setPlayerScores(game!!.playerScore1, game!!.playerScore2)
                if (game!!.finalGameResult() == Result.RUNNING) {
                    userActionsBlocked = true
                    animGameScreen.roundEnded(false, event.winner == playerIdx)
                } else if (queue.isNotEmpty()) handleNextEvent()
            }
            is GameEndEvent -> {
                userActionsBlocked = true
                animGameScreen.roundEnded(true, event.winner == playerIdx)
            }
        }
    }

    override fun newRoundClicked() {
        if (game == null) return
        game!!.startNewRound()
    }

    override fun animationFinished() {
        userActionsBlocked = false
        // if the event being handled was a dice roll or a move, we highlight the board
        if (turnOf == playerIdx && (currentEvent is DiceRollEvent || currentEvent is MoveEvent)) {
            highlightBoard()
        }
        // if no moves are possible, display the no moves message
        if (turnOf == playerIdx && currentEvent is DiceRollEvent && game!!.turnOf() != turnOf) {
            userActionsBlocked = true
            currentEvent = null
            animGameScreen.noMovesAvailable()
            return
        }
        // round has ended, start new round
        if (currentEvent is RoundEndEvent && game!!.finalGameResult() == Result.RUNNING) {
            currentEvent = null
            queue.clear()
            game!!.startNewRound()
        }
        // game has ended, restart
        if (currentEvent is GameEndEvent) start()
        if (queue.isNotEmpty()) handleNextEvent()
    }

    private fun handleComputerResponse(moves: List<Move>) {
        for (move in moves) {
            game!!.makeMove(move.from, move.to)
        }
    }

    override fun fieldClicked(idx: Int) {
        // the game is not running or the user is not allowed to act
        checkNotNull(game)
        if (userActionsBlocked || game!!.currentRoundResult() != Result.RUNNING) return
        // if not the player's turn or dice roll has not been performed yet, clicking is disallowed
        if (turnOf != playerIdx || game!!.dice == null) return

        val moveableFields = game!!.moveableFields()

        // if no fields are selected yet, and the player can move from `idx`, select it
        if (fieldSelected == null && idx in moveableFields) {
            fieldSelected = idx
            highlightBoard()
        } else if (fieldSelected != null) {
            val possibleFields = game!!.possibleFieldsFrom(fieldSelected!!)

            // if the move is invalid, but moves from the clicked field are available, select the new field
            if (idx != fieldSelected && idx !in possibleFields && idx in moveableFields) {
                fieldSelected = idx
                highlightBoard()
            } else if (idx in possibleFields) {
                // The move is made and selection is cleared
                game!!.makeMove(fieldSelected!!, idx)
                highlightBoard(false)
            } else {
                fieldSelected = null
                highlightBoard()
            }
        }
    }

    override fun checkerClicked(fieldIdx: Int) {
        fieldClicked(fieldIdx)
    }

    override fun bearOffClicked() {
        checkNotNull(game) {"Game is null, and bearOffClicked() called."}
        if (userActionsBlocked || game!!.currentRoundResult() != Result.RUNNING) return

        fieldSelected?.let {
            if (game!!.isMovePossible(it, -1)) {
                game!!.makeMove(it, -1)
                highlightBoard(false)
                return
            }
        }
        fieldSelected = null
        highlightBoard(true)
    }

    override fun diceRollClicked() {
        checkNotNull(game) {"Game is null, and dice roll is requested."}
        if (userActionsBlocked) return
        val currentResult = game!!.currentRoundResult()
        if (currentResult == Result.NOT_STARTED) game!!.startNewRound()
        else if (currentResult == Result.RUNNING && turnOf == playerIdx) {
            game!!.rollDice()
        }
    }

    override fun backgroundClicked() {
        if (fieldSelected == null) return
        checkNotNull(game)
        fieldSelected = null
        if (game!!.currentRoundResult() == Result.RUNNING && turnOf == playerIdx) highlightBoard()
        else highlightBoard(false)
    }
}