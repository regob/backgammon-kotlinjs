package ai

import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessageEvent
import kotlin.random.Random

/**
 * Global scope of the WebWorker
 * see: https://developer.mozilla.org/en-US/docs/Web/API/DedicatedWorkerGlobalScope
 */
external val self: DedicatedWorkerGlobalScope


fun makeSuggestion(messageEvent: MessageEvent) {
    console.log("Worker got query")
    val query: QueryData = JSON.parse(messageEvent.data as String)
    val moves = query.gameState.possibleMoveSequences(4)
    val choice = if (moves.isEmpty()) listOf() else moves[Random.nextInt(0, moves.size)]
    val response = ResponseData(choice)
    self.postMessage(JSON.stringify(response))
    console.log("Worker sent response: choice")
}

fun main() {
    self.onmessage = ::makeSuggestion
}