package model

/**
 * Event interface of the Game. Data contained for each event type:
 * - NewRoundEvent: -
 * - InitialDiceRollEvent: List<Dice> (one Dice stores a roll of both players)
 * - NewTurnEvent: Int (who's turn)
 * - DiceRollEvent: Dice
 * - MoveEvent: Move
 * - RoundEndEvent: Int (who won)
 * - GameEndEvent: Int (who won)
 */
open class GameEvent
class NewRoundEvent :GameEvent()

/**
 * Initial dice roll. One Dice object stores one roll of each player:
 * num1 is player1's roll and num2 is player2's.
 * If there are n rolls, it means that the first n-1 were equal (num1 == num2).
 */
class InitialDiceRollEvent(val dice: List<Dice>) :GameEvent()

class NewTurnEvent(val turnOf: Int) :GameEvent()
class DiceRollEvent(val dice: Dice) :GameEvent()
class MoveEvent(val move: Move) :GameEvent()
class RoundEndEvent(val winner: Int) :GameEvent()
class GameEndEvent(val winner: Int) :GameEvent()




