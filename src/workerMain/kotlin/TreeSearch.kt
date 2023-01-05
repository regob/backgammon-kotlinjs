import kotlin.js.Date
import kotlin.math.*

private val diceWeight: Map<Dice, Int> = Dice.allDice()
    .map {
        if (it.num1 > it.num2) Dice(it.num2, it.num1) else it
    }
    .groupingBy { it }.eachCount()
private val uniqueDice = diceWeight.keys.toList()
private val totalDiceWeight = Dice.allDice().count()

class HeuristicScorer {

    val MX: Int = 300

    val winProb: Array<Array<Double>> = run {
        val P = Array(MX + 1) {Array(MX + 1) {1.0} }

        // iterate row and column i simultaneously
        for (i in 0..MX) {
            // start at (i, i)
            for (j in i..MX) {
                // compute P[i][j]
                var total = 0.0
                for ((dice, weight) in diceWeight.entries) {
                    // recurse to (j - dice.sum(), i)
                    val col = i - dice.numbers().sum()
                    if (col <= 0) total += weight
                    else total += (1 - P[j][col]) * weight
                }
                P[i][j] = total / totalDiceWeight

                // compute P[j][i]
                total = 0.0
                for ((dice, weight) in diceWeight.entries) {
                    // recurse to (i, j - dice.sum())
                    val col = j - dice.numbers().sum()
                    if (col <= 0) total += weight
                    else total += (1 - P[i][col]) * weight
                }
                P[j][i] = total / totalDiceWeight

            }
        }

        P
    }

    fun score(state: GameState, maximizingPlayer: Int): Double {
        val movesNeeded = mutableListOf(0, 0, 0)
        val worstHitForNum = MutableList(3) { MutableList(7) {0} }

        // distance of the two farthest checkers from the bear off point for the current player
        val highestDists = mutableListOf(0, 0)

        for (i in 0..25) {
            val (ply, cnt) = state.fieldPlayerIdx[i] to state.fields[i]
            val opponent = 3 - ply
            if (ply == 0 || cnt == 0) continue

            val moves = if (ply == 1) 25 - i else i
            movesNeeded[ply] += moves * cnt


            // if only one checker here, check possible hits by the opponent
            if (cnt == 1) {
                for (dist in 1..6) {
                    val from = if (ply == 1) i + dist else i - dist
                    if (from < 0 || from > 25) break

                    if (state.fieldPlayerIdx[from] == opponent)
                        worstHitForNum[ply][dist] = max(
                            worstHitForNum[ply][dist],
                            if (ply == 1) i else 25 - i
                        )
                }
            }


            if (ply == state.turnOf && highestDists[1] == 0) {
                val dist = if (ply == 1) 25 - i else i

                for (idx in 0 until state.fields[i]) {
                    if (highestDists[0] == 0) highestDists[0] = dist
                    else if (highestDists[1] == 0) highestDists[1] = dist
                    else break
                }
            }
        }

        // for each player, if it has pieces on the bar, add possible wasted numbers
        for (player in 1..2) {
            val opp = 3 - player
            val bar = if (player == 1) 0 else 25
            if (state.fields[bar] == 0) continue
            val badFields = mutableListOf<Int>()
            for (i in 1..6) {
                val target = if (player == 1) bar + i else bar - i
                // opponent has more than one checkers on target field: add to bad fields
                if (state.fieldPlayerIdx[target] == opp && state.fields[target] > 1) badFields.add(i)
            }

            var totalPenalty = 0
            for ((dice, weight) in diceWeight.entries) {
                if (state.fields[bar] == 1) {
                    if (dice.numbers().all {it in badFields}) totalPenalty += dice.numbers().sum() * weight
                } else if (dice.numbers().any {it in badFields}) totalPenalty += dice.numbers().sum() * weight
            }

            movesNeeded[player] += totalPenalty / totalDiceWeight
        }

        // for each player consider the single worst piece that can be hit by the opponent for each die number
        // add the average of the position lost when that checker gets hit to movesNeeded
        for (player in 1..2) {
            // if the dice are known for the opponent -- use those for calculation
            if (state.turnOf != player && state.numbersLeft.isNotEmpty()) {
                for (num in state.numbersLeft.take(2))
                    movesNeeded[player] += worstHitForNum[player][num] / 2
            } else {
                // player is on turn or opponent's numbers are unknown
                for (num in 1..6)
                    movesNeeded[player] += worstHitForNum[player][num] / 6
            }
        }

        // subtract dice values from current player's movesNeeded, as the player will
        // use them to move, and reduce his/her count (if possible)
        if (state.numbersLeft.size >= 2) {
            if (highestDists.max() > 6) movesNeeded[state.turnOf] -= min(movesNeeded[state.turnOf], state.numbersLeft.sum())
            else {
                // player is bearing off -> cap the numbers subtracted by the highest distances

                // only check the first two dice
                var (n1, n2) = state.numbersLeft
                if (n1 < n2) n1 = n2.also { n2 = n1 }

                val sub = min(highestDists[0], n1) + min(highestDists[1], n2)
                movesNeeded[state.turnOf] -= sub
            }
        }

        val opponent = 3 - state.turnOf

        // maximize movesNeeded values in MX
        for (i in 1..2) movesNeeded[i] = min(MX, movesNeeded[i])

        // probability that opponent wins
        val pOpponent = if (state.numbersLeft.size == 0) {
            // player has not rolled the dice yet, compute prob for current player going first
            1.0 - winProb[movesNeeded[state.turnOf]][movesNeeded[opponent]]
        } else winProb[movesNeeded[opponent]][movesNeeded[state.turnOf]]

        return if (maximizingPlayer == opponent) pOpponent else 1 - pOpponent
    }

}

fun GameState.uniqueMoveSequences(): List<List<Move>> {
    val moveSequences = possibleMoveSequences()
    // remove duplicate move sets, i.e. move sequences that are included in multiple orderings
    // e.g Move(1, 6), Move(4, 7) and Move(4, 7), Move(1, 6)
    val moveSet = mutableSetOf<Set<Move>>()
    return moveSequences.filter {
        val s = it.toSet()
        if (s in moveSet) false else {
            moveSet.add(s)
            true
        }
    }.ifEmpty { listOf(emptyList()) }
}

fun GameState.terminalValue(maximizingPlayer: Int): Double? {
    val result = roundResult()
    if (result == Result.PLAYER1_WON) return if (maximizingPlayer == 1) 1.0 else 0.0
    if (result == Result.PLAYER2_WON) return if (maximizingPlayer == 1) 0.0 else 1.0
    return null
}

abstract class Node(var children: List<Node>?, var isTerminal: Boolean, var value: Double?) {
    abstract fun expand(state: GameState, maximizingPlayer: Int, depth: Int, endTime: Double)
}

class MiniMaxNode(val maximize: Boolean): Node(null, false, null) {

    var actionPerChildren: List<List<Move>>? = null

    override fun expand(state: GameState, maximizingPlayer: Int, depth: Int, endTime: Double) {
        if (Date.now() >= endTime) {
            value = null
            return
        }

        // depth is 0, so we have to score this state
        if (depth == 0) {
            value = scorer.score(state, maximizingPlayer)
            return
        }
        value = null

        // node not yet expanded: generate children
        if (children == null) {
            actionPerChildren = state.uniqueMoveSequences()
            children = List(actionPerChildren!!.size) { ChanceNode(!maximize) }
        }

        var best = if (maximize) 0.0 else 1.0
        for (i in children!!.indices) {
            for (move in actionPerChildren!![i]) state.forceMove(move)
            state.nextTurn()

            children!![i].expand(state, maximizingPlayer, depth - 1, endTime)
            if (children!![i].value == null) return
            if (maximize) best = max(best, children!![i].value!!)
            else best = min(best, children!![i].value!!)

            state.undoLastTurn()
            repeat(actionPerChildren!![i].size) {state.undoLastMove()}
        }
        value = best
    }

}

class ChanceNode(val childrenAreMax: Boolean): Node(null, false, null) {

    override fun expand(state: GameState, maximizingPlayer: Int, depth: Int, endTime: Double) {
        if (Date.now() >= endTime) {
            value = null
            return
        }

        value = state.terminalValue(maximizingPlayer)
        // state is terminal state, we can assign the  utility value to this node
        if (value != null) {
            isTerminal = true
            return
        }

        // depth == 0: we evaluate this state
        if (depth == 0) {
            value = scorer.score(state, maximizingPlayer)
            return
        }

        // not expanded yet
        if (children == null) {
            children = List(uniqueDice.size) {MiniMaxNode(childrenAreMax)}
        }

        // expand children
        var sumValues = 0.0
        for (i in children!!.indices) {
            state.setDice(uniqueDice[i])

            children!![i].expand(state, maximizingPlayer, depth - 1, endTime)
            if (children!![i].value == null) return
            sumValues += children!![i].value!! * diceWeight[uniqueDice[i]]!!.toDouble()
        }
        value = sumValues / totalDiceWeight
    }
}

fun iterativeDeepeningSearch(root: MiniMaxNode, state: GameState, timelimitMs: Int, startDepth: Int = 1, maxDepth: Int = 1000): List<Move> {
    val startTime = Date.now()
    val endTime = startTime + timelimitMs
    var bestVal = -1.0
    var bestAction: List<Move>? = null

    for (d in startDepth..maxDepth) {
        root.expand(state, state.turnOf, d, endTime)
        println("Depth $d ready at ${Date.now() - startTime}")
        if (root.value == null) break
        bestVal = -1.0
        for ((child, action) in root.children!!.zip(root.actionPerChildren!!)) {
            if (child.value!! > bestVal) {
                bestVal = child.value!!
                bestAction = action
            }
        }
        println("Depth: $d bestVal: $bestVal bestAction: $bestAction")
    }
    return bestAction ?: emptyList()
}
