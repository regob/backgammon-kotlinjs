package model

import kotlin.math.abs
import kotlin.math.max

/**
 * A move of a checker from a field to another field.
 */
open class Move(val from: Int, val to: Int) {
    override fun toString(): String {
        return "Move($from, $to)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Move) return false
        return from == other.from && to == other.to
    }

    override fun hashCode(): Int {
        return Pair(from, to).hashCode()
    }
}

/**
 * A move performed in the game.
 */
class MoveMade(
    fieldFrom: Int, fieldTo: Int, val hitPiece: Boolean, val numberUsed: Int, val withPrevious: Boolean = false
) : Move(fieldFrom, fieldTo)

/**
 * ## State of the backgammon game's current round.
 * Normal fields are numbered from 1 to 24, starting from bottom right.
 * Player1's home row is 18-24, player2's is 1-6.
 * Fields 0 and 25 are the bars of player1 and player2 respectively.
 * Field -1 is the exit point for both players.
 */
data class GameState(
    var turnOf: Int, val fields: MutableList<Int>, val fieldPlayerIdx: MutableList<Int>,
    val numbersLeft: MutableList<Int> = mutableListOf(),
) {

    private val lastMovesByTurn: MutableList<MutableList<MoveMade>> = mutableListOf(
        mutableListOf()
    )

    private inline fun opponent() = 3 - turnOf

    override fun equals(other: Any?): Boolean {
        if (other !is GameState) return false
        return turnOf == other.turnOf && fields == other.fields &&
                fieldPlayerIdx == other.fieldPlayerIdx && numbersLeft.sorted() == other.numbersLeft.sorted()
    }

    override fun hashCode(): Int {
        return turnOf.hashCode() * fields.hashCode() * fieldPlayerIdx.hashCode() * numbersLeft.hashCode()
    }

    /**
     * Start the next turn. The current player is set to be the opponent.
     */
    fun nextTurn() {
        turnOf = opponent()
        lastMovesByTurn.add(mutableListOf())
    }

    /**
     * Set the numbers from the dice rolled this turn.
     */
    fun setDice(dice: Dice) {
        numbersLeft.clear()
        numbersLeft.addAll(dice.numbers())
    }

    /**
     * Create a deep copy of the state. The last moves are not copied.
     */
    fun deepcopy(): GameState {
        return copy(
            fields = fields.toMutableList(),
            fieldPlayerIdx = fieldPlayerIdx.toMutableList(),
            numbersLeft = numbersLeft.toMutableList()
        )
    }

    /**
     *  Check whether the current player meets the criteria for bearing off.
     */
    private fun bearOffPossible(): Boolean {
        val badFields = if (turnOf == 1) 0..18 else 7..25
        for (i in badFields)
            if (fieldPlayerIdx[i] == turnOf && fields[i] > 0) return false
        return true
    }

    /**
     * Check whether a move can be performed in a single move, regardless of the dice.
     */
    private fun isMoveLegalWithoutDice(move: Move): Boolean {
        // the source field is not the current player's
        if (fieldPlayerIdx[move.from] != turnOf || fields[move.from] == 0) return false

        // bearing off
        if (move.to == -1) {
            if (!bearOffPossible()) return false
            return if (turnOf == 1) move.from > 18 else move.from < 7
        }

        val stepsize = if (turnOf == 1) move.to - move.from else move.from - move.to
        // too big or non-positive step
        if (stepsize <= 0 || stepsize > 6) return false

        // the opponent has multiple pieces on the target field
        if (fieldPlayerIdx[move.to] != turnOf && fields[move.to] > 1) return false
        // the target is the bar ...
        if (move.to < 1 || move.to > 24) return false

        // the source is the bar but not the current player's
        if ((move.from == 0 && turnOf == 2) || (move.from == 25 && turnOf == 1)) return false

        val playerBar = if (turnOf == 1) 0 else 25
        // the source is not the bar, but the player has pieces on the bar
        if (move.from != playerBar && fields[playerBar] > 0) return false
        return true
    }

    /**
     * Check whether the move can be performed using `number` from the die.
     */
    private fun isMoveLegalUsingNumber(move: Move, number: Int): Boolean {
        // illegal move on the board
        if (!isMoveLegalWithoutDice(move)) return false
        // if it is not a bearing off move, the number has to equal the field diff
        if (move.to >= 0) return number == abs(move.from - move.to)
        // the move is a bearing off move at this point (move.to == -1)

        val target = move.from + (if (turnOf == 1) number else -number)
        val bearOffDiff = if (turnOf == 1) 25 - target else target
        if (bearOffDiff == 0) return true
        if (bearOffDiff > 0) return false
        // `number` is more than what is needed. No non-bear off moves should exist.

        val higherRange = if (turnOf == 1) 19 until move.from else 6 downTo (move.from + 1)
        // go through the higher home row fields, where shouldn't be valid moves
        for (i in higherRange) {
            if (fields[i] == 0 || fieldPlayerIdx[i] != turnOf) continue

            // check for each number whether a move is possible from `i` to `i + num` or -1
            for (num in numbersLeft.toSet()) {
                val tar = i + if (turnOf == 1) num else -num
                // bear-off available
                if (tar >= 25 || tar <= 0) return false
                // ordinary move available
                if (fields[tar] < 2 || fieldPlayerIdx[tar] == turnOf) return false
            }
        }
        return true
    }

    /**
     * Candidate moves that can be made currently if dice rules allow. Non-exact bear off moves excluded.
     */
    private fun moveCandidates(dieNum: Int): List<Move> {
        val range = if (turnOf == 1) 0..(24 - dieNum) else (dieNum + 1)..25
        val diff = if (turnOf == 1) dieNum else -dieNum
        val moves = mutableListOf<Move>()
        // check normal moves
        for (i in range) {
            val move = Move(i, i + diff)
            if (isMoveLegalUsingNumber(move, dieNum)) moves.add(move)
        }

        // check exact bearing off too
        val field = if (turnOf == 1) 25 - dieNum else dieNum
        if (isMoveLegalUsingNumber(Move(field, -1), dieNum)) moves.add(Move(field, -1))

        return moves
    }

    private fun nonExactBearOffCandidate(): Move? {
        val maxNum = numbersLeft.max()
        // go through the available bear off fields and get the farthest one
        val bearOffRange = if (turnOf == 1) (26 - maxNum)..24 else (maxNum - 1) downTo 1
        for (i in bearOffRange) {
            if (fields[i] > 0 && fieldPlayerIdx[i] == turnOf) {
                val move = Move(i, -1)
                return if (isMoveLegalUsingNumber(move, maxNum)) move else null
            }

        }
        return null
    }

    /**
     * Returns the possible sequences of moves that can be played currently.
     */
    fun possibleMoveSequences(maxLen: Int): List<List<Move>> {
        check(maxLen >= 2) { "maxLen has to be at least 2 for deciding which moves are valid" }
        if (numbersLeft.isEmpty()) return emptyList()

        /**
         * Walk the tree of possible moves with DFS
         */
        fun dfs(
            numbers: List<Int>,
            path: MutableList<Move> = mutableListOf(),
            acc: MutableList<List<Move>> = mutableListOf(),
        ): MutableList<List<Move>> {
            if (numbers.isEmpty() || path.size == maxLen) {
                acc.add(path.toList())
                return acc
            }
            val initialAccSize = acc.size

            // check possible moves for all unique die numbers left
            val candidates = mutableMapOf<Int, List<Move>>()
            for (num in numbers.toSet()) candidates[num] = moveCandidates(num)
            // if no legal candidate was found, we check the non-exact bear off move
            if (candidates.all {it.value.isEmpty()}) {
                val cand = nonExactBearOffCandidate()
                if (cand != null) candidates[numbers.max()] = listOf(cand)
            }

            // check each possible move's subtree
            for ((num, moves) in candidates.entries) {
                val numIdx = numbers.indexOf(num)
                val remainingNumbers = numbers.take(numIdx) + numbers.drop(numIdx + 1)
                for (move in moves) {
                    doMakeMove(move, num)
                    path.add(move)
                    dfs(remainingNumbers, path, acc)
                    path.removeLast()
                    doUndoLastMove()
                }
            }

            // if no children node was inserted, we insert this node, since it is possible that
            // not all numbers can be used, and we need the longest paths
            if (initialAccSize == acc.size && path.size > 0) acc.add(path.toList())
            return acc
        }

        val candidates = dfs(numbersLeft)
        if (candidates.isEmpty()) return candidates
        val maxCandidateLen = candidates.maxOf { it.size }
        // only the longest move sequences are valid (player has to use the most possible dice numbers)
        val longestCandidates = candidates.filter { it.size == maxCandidateLen }
        // if only one move can be done, we have to use the bigger number
        if (maxCandidateLen == 1 && numbersLeft.size > 1) {
            val maxNum = numbersLeft.max()
            return longestCandidates.filter { isMoveLegalUsingNumber(it[0], maxNum) }
        }
        return longestCandidates
    }

    /**
     * If the move is a compound move, split it to single moves. If it is invalid, return null.
     */
    private fun decomposeMove(move: Move): List<Move>? {
        val moveSequences = possibleMoveSequences(4)
        // we search for the shortest good move sequence that equals to this move
        var shortestSeq: List<Move>? = null
        for (moveSeq in moveSequences) {
            var from = move.from
            for ((i, m) in moveSeq.withIndex()) {
                if (m.from != from) break
                if (m.to == move.to) {
                    val seq = moveSeq.subList(0, i + 1)
                    if (seq.size == 1) return seq
                    if (shortestSeq == null || shortestSeq.size > seq.size) shortestSeq = seq
                }
                from = m.to
            }
        }
        return shortestSeq
    }

    /**
     * Check whether a move is legal in the game.
     *  The move can be a `compound move` too (multiple moves with the same piece, e.g. field 1 -> field 4 -> field 8)
     */
    fun isMovePossible(move: Move): Boolean {
        return decomposeMove(move) != null
    }

    /**
     * Return fields from which the player has available moves.
     */
    fun moveableFields(): List<Int> {
        val moveSequences = possibleMoveSequences(2)
        val fields = mutableSetOf<Int>()
        for (moveSeq in moveSequences) {
            fields.add(moveSeq[0].from)
        }
        return fields.toList()
    }

    /**
     * Check whether any moves are available for the current player
     */
    fun anyMovesPossible(): Boolean {
        return possibleMoveSequences(2).isNotEmpty()
    }

    fun possibleMovesFrom(field: Int): List<Move> {
        val moveSequences = possibleMoveSequences(4)
        val moves = mutableSetOf<Move>()
        for (moveSeq in moveSequences) {
            var from = field
            for (move in moveSeq) {
                if (move.from != from) break
                moves.add(Move(field, move.to))
                from = move.to
            }
        }
        return moves.toList()
    }

    private fun doMakeMove(move: Move, dieNum: Int, withPrevious: Boolean = false) {
        fields[move.from] -= 1
        if (fields[move.from] == 0) fieldPlayerIdx[move.from] = 0
        var hitPiece = false
        if (move.to >= 0) {
            hitPiece = (fields[move.to] == 1 && fieldPlayerIdx[move.to] != turnOf)
            if (hitPiece) {
                val barField = if (turnOf == 1) 25 else 0
                fields[barField] += 1
                fieldPlayerIdx[barField] = opponent()
            } else fields[move.to] += 1
            fieldPlayerIdx[move.to] = turnOf
        }
        lastMovesByTurn.last().add(MoveMade(move.from, move.to, hitPiece, dieNum, withPrevious))
        numbersLeft.remove(dieNum)
    }

    fun makeMove(move: Move) {
        check(numbersLeft.size > 0)
        val singleMoves = decomposeMove(move)
        singleMoves ?: return

        for ((i, sMove) in singleMoves.withIndex()) {
            val (highNum, lowNum) = Pair(numbersLeft.max(), numbersLeft.min())
            // try to make the move with the highest number first
            if (isMoveLegalUsingNumber(sMove, highNum)) {
                doMakeMove(sMove, highNum, i > 0)
            } else {
                doMakeMove(sMove, lowNum, i > 0)
            }
        }
    }

    private fun doUndoLastMove(): Boolean {
        val move = lastMovesByTurn.lastOrNull()?.removeLastOrNull() ?: return false

        if (move.hitPiece) { // hit a piece ==> we have to restore that piece
            fieldPlayerIdx[move.to] = opponent()
            val bar = if (turnOf == 1) 25 else 0
            fields[bar] -= 1
            if (fields[bar] == 0) fieldPlayerIdx[bar] = 0
        } else if (move.to >= 0) {
            // not a bear-off move, and didn't hit a checker either ==> remove a checker from field `to`
            fields[move.to] -= 1
            if (fields[move.to] == 0) fieldPlayerIdx[move.to] = 0
        }
        fieldPlayerIdx[move.from] = turnOf
        fields[move.from] += 1
        numbersLeft.add(move.numberUsed)
        return true
    }

    fun undoLastMove(): Boolean {
        val lastMoves = lastMovesByTurn.lastOrNull()
        lastMoves ?: return false
        if (lastMoves.size == 0) return false
        do {
            val lastMove = lastMoves.last()
            doUndoLastMove()
        } while (lastMove.withPrevious)
        return true
    }

    fun undoLastTurn(): Boolean {
        val lastMoves = lastMovesByTurn.lastOrNull()
        lastMoves ?: return false
        while (doUndoLastMove()) {}
        lastMovesByTurn.removeLast()
        numbersLeft.clear()
        turnOf = opponent()
        return true
    }

    fun roundResult(): Result {
        if ((0..24).all { fieldPlayerIdx[it] != 1 || fields[it] == 0 }) return Result.PLAYER1_WON
        if ((1..25).all { fieldPlayerIdx[it] != 2 || fields[it] == 0 }) return Result.PLAYER2_WON
        return Result.RUNNING
    }

}

fun initialGameState(whoGoesFirst: Int): GameState {
    val fields = MutableList(26) { 0 }
    val fieldPlayerIdx = MutableList(26) { 0 }
    val initialPlayer1 = listOf(Pair(1, 2), Pair(12, 5), Pair(17, 3), Pair(19, 5))
    for ((field, cnt) in initialPlayer1) {
        fields[field] = cnt
        fieldPlayerIdx[field] = 1
        fields[25 - field] = cnt
        fieldPlayerIdx[25 - field] = 2
    }
    return GameState(whoGoesFirst, fields, fieldPlayerIdx)
}

fun asciiBoard(gameState: GameState): String {
    val rows = mutableListOf<String>()
    for (i in 1..3) {
        val items = mutableListOf<String>()
        for (x in 13..24) {
            var item = if (gameState.fields[x] >= i) " ${gameState.fieldPlayerIdx[x]} " else "   "
            if (i == 3 && gameState.fields[x] > 3) item = "(${gameState.fields[x] - 2})"
            items.add(item)
            if (x == 18) items.add("|   |")
        }
        rows.add("|${items.joinToString("|")}|\n")
    }
    val padding = 4*6 + 1
    rows.add("|   |".padEnd(5 + padding).padStart(5 + 2*padding) + "\n")

    val diceNums = "${gameState.numbersLeft}"
    var center = "|(${gameState.fields[25]})|".padEnd(5 + padding).padStart(5 + 2*padding) + "\n"
    if (gameState.turnOf == 1) {
        center = center.replaceRange(0, diceNums.length, diceNums)
    } else center = center.replaceRange(center.length - 1 - diceNums.length, center.length -1, diceNums)
    rows.add(center)
    rows.add("|(${gameState.fields[0]})|".padEnd(5 + padding).padStart(5 + 2*padding) + "\n")
    rows.add("|   |".padEnd(5 + padding).padStart(5 + 2*padding) + "\n")

    for (i in 3 downTo 1) {
        val items = mutableListOf<String>()
        for (x in 12 downTo 1) {
            var item = if (gameState.fields[x] >= i) " ${gameState.fieldPlayerIdx[x]} " else "   "
            if (i == 3 && gameState.fields[x] > 3) item = "(${gameState.fields[x] - 2})"
            items.add(item)
            if (x == 7) items.add("|   |")
        }
        rows.add("|${items.joinToString("|")}|\n")
    }
    rows.add(0, "".padEnd(rows[0].length - 1, '-') + "\n")
    rows.add(rows.size, "".padEnd(rows[0].length - 1, '-') + "\n")
    return rows.joinToString("")
}