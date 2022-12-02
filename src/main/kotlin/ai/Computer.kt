package ai

import model.Dice
import model.GameState
import model.Move
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker

/**
 * Query data from Computer to Worker
 */
class QueryData(
    val gameState: GameState,
    val level: Int,
    val timeLimitMs: Int
)

/**
 * Response data from Worker
 */
class ResponseData(
    val moves: List<Move>
)

/**
 * Represents an AI player
 */
class Computer (level: Int, val callback: (List<Move>) -> Unit, val timeLimitMs: Int =  500) {
    val level: Int = run {
        if (level !in 1..5) throw IllegalArgumentException("Invalid computer level: $level")
        level
    }

    private val worker: Worker = Worker("AiWorker.js")

    init {
        worker.onmessage = {processWorkerResult(it)}
    }

    fun query(gameState: GameState) {
        val data = QueryData(gameState, level, timeLimitMs)
        worker.postMessage(JSON.stringify(data))
    }

    private fun processWorkerResult(messageEvent: MessageEvent) {
        val data: ResponseData = JSON.parse(messageEvent.data as String)
        callback(data.moves)
    }

}
