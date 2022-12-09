package model

import kotlinx.browser.localStorage
import kotlin.math.max
import kotlin.math.min

enum class Result {
    NOT_STARTED,
    RUNNING,
    PLAYER1_WON,
    PLAYER2_WON,
}


class Game(
    val numGames: Int,
    var gameEventListener: ((GameEvent) -> Unit)? = null,
) {
    var gameState: GameState? = null
        private set
    var dice: Dice? = null
        private set
    var playerScore1: Int = 0
        private set
    var playerScore2: Int = 0
        private set
    var gameIdx: Int = 0
        private set

    fun turnOf(): Int? {
        return gameState?.turnOf
    }

    private fun raiseEvent(event: GameEvent) {
        gameEventListener?.invoke(event)
    }

    fun currentRoundResult(): Result {
        return gameState?.roundResult() ?: Result.NOT_STARTED
    }

    fun finalGameResult(): Result {
        if (gameIdx <= 0) return Result.NOT_STARTED
        val currentResult = currentRoundResult()
        if (currentResult in listOf(Result.PLAYER1_WON, Result.PLAYER2_WON)) {
            val minScore = min(playerScore1, playerScore2)
            val maxScore = max(playerScore1, playerScore2)
            if (minScore + numGames - gameIdx < maxScore)
                return if (playerScore1 > playerScore2) Result.PLAYER1_WON else Result.PLAYER2_WON
        }
        return Result.RUNNING
    }

    fun startNewRound() {
        if (currentRoundResult() == Result.RUNNING) throw IllegalStateException("A round is already running.")
        if (finalGameResult() !in listOf(Result.NOT_STARTED, Result.RUNNING))
            throw IllegalStateException("Game finished, cannot start a new round.")
        gameIdx += 1
        val rolls = mutableListOf<Dice>()
        while (rolls.isEmpty() || rolls.last().num1 == rolls.last().num2)
            rolls.add(Dice.roll())
        val finalRoll = rolls.last()
        gameState = initialGameState(if (finalRoll.num1 > finalRoll.num2) 1 else 2)
        raiseEvent(NewRoundEvent())
        raiseEvent(InitialDiceRollEvent(rolls))
        raiseEvent(NewTurnEvent(gameState!!.turnOf))
        dice = null
    }

    fun rollDice() {
        if (currentRoundResult() != Result.RUNNING) throw IllegalStateException("Game not running, cannot roll dice.")
        dice = Dice.roll()
        gameState?.setDice(dice!!)
        raiseEvent(DiceRollEvent(dice!!))

        // if no moves are possible after the roll, next turn is performed
        if (gameState!!.anyMovesPossible()) return
        nextTurn()
    }

    private fun nextTurn() {
        checkNotNull(gameState) {"gameState is null, nextTurn() call invalid"}
        dice = null
        gameState!!.nextTurn()
        raiseEvent(NewTurnEvent(gameState!!.turnOf))
    }

    fun isMovePossible(fieldFrom: Int, fieldTo: Int): Boolean {
        checkNotNull(gameState) {"GameState is null, isMovePossible called"}
        return gameState!!.isMovePossible(Move(fieldFrom, fieldTo))
    }

    fun moveableFields(): List<Int> {
        checkNotNull(gameState) {"GameState is null, isMovePossible called"}
        return gameState!!.moveableFields()
    }

    fun possibleFieldsFrom(field: Int) : List<Int> {
        checkNotNull(gameState) {"GameState is null, possibleFieldsFrom called"}
        val moves = gameState!!.possibleMovesFrom(field)
        return moves.map {it.to}
    }

    fun playerControlsField(fieldIdx: Int): Boolean {
        checkNotNull(gameState) {"GameState is null, playerControlsField called"}
        return gameState!!.turnOf == gameState!!.fieldPlayerIdx[fieldIdx]
    }

    fun makeMove(fieldFrom: Int, fieldTo: Int) {
        if (!isMovePossible(fieldFrom, fieldTo)) {
            throw IllegalArgumentException("Move impossible from $fieldFrom to $fieldTo")
        }
        val move = Move(fieldFrom, fieldTo)
        val singleMoves = gameState!!.decomposeMove(move)
        gameState!!.makeMove(move)
        for (m in singleMoves!!) raiseEvent(MoveEvent(m))

        // if no moves left, check game end and go to next turn
        if (gameState!!.anyMovesPossible()) return
        val roundResult = currentRoundResult()
        if (roundResult != Result.RUNNING) {
            val winner = if (roundResult == Result.PLAYER1_WON) 1 else 2
            if (winner == 1) playerScore1 += 1
            else playerScore2 += 1
            raiseEvent(RoundEndEvent(winner))

            // check if game ended
            val gameResult = finalGameResult()
            if (gameResult != Result.RUNNING) {
                val gameWinner = if (gameResult == Result.PLAYER1_WON) 1 else 2
                raiseEvent(GameEndEvent(gameWinner))
            }

            return
        }
        // round has not ended, go to next turn
        nextTurn()
    }


    fun loadGameState(state: GameState, score1: Int, score2: Int, gameIdx: Int) {
        playerScore1 = score1
        playerScore2 = score2
        this.dice = dice
        gameState = state
        this.gameIdx = gameIdx
    }



    fun save() {
        val obj = mapOf(
//            "playerIdx" to playerIdx,
//            "computerLevel" to computerLevel,
            "gameState" to gameState,
            "playerScore1" to playerScore1,
            "playerScore2" to playerScore2,
            "dice" to dice,
            "numGames" to numGames,
            "gameIdx" to gameIdx
        )
        localStorage.setItem("game", JSON.stringify(obj))
    }
}


fun loadGame(): Game? {
    val saved = localStorage.getItem("game") ?: return null
    val obj: Map<String, Any?> = JSON.parse(saved)
//    val game =  Game(
//        obj["playerIdx"] as Int,
//        obj["computerLevel"] as Int,
//        obj["numGames"] as Int
//    )
//    game.loadGameState(
//        obj["gameState"] as GameState,
//        obj["dice"] as Dice,
//        obj["playerScore1"] as Int,
//        obj["playerScore2"] as Int,
//        obj["gameIdx"] as Int,
//    )
    return null
}

