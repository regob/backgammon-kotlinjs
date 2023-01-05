import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessageEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

external val self: DedicatedWorkerGlobalScope

val scorer = HeuristicScorer()

fun makeSuggestion(messageEvent: MessageEvent) {
    console.log("Worker got query")
    val query: QueryData = Json.decodeFromString(messageEvent.data as String)

    val score = scorer.score(query.gameState, query.gameState.turnOf)
    console.log("Computer's score: $score")

    val root = MiniMaxNode(true)
    val action = iterativeDeepeningSearch(root, query.gameState, query.timeLimitMs)
    val response = ResponseData(action)

    self.postMessage(Json.encodeToString(response))
    console.log("Worker sent response: $action")
}

fun main() {
    self.onmessage = ::makeSuggestion
}