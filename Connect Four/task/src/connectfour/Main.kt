package connectfour

import kotlin.properties.Delegates

class Game {
    // TODO: seald Player1(name) Player2(name)
    lateinit var firstPlayerName: String
    lateinit var secondPlayerName: String
    var totalGames = 1
    var currentGame = 1
    var firstPlayerScore = 0
    var secondPlayerScore = 0

    // TODO: separate OneGame and GameSession
    var height by Delegates.notNull<Int>()
    var width by Delegates.notNull<Int>()

    lateinit var board: Map<Int, MutableList<Char>>

    fun initBoard() {
        board = HashMap<Int, MutableList<Char>>().apply {
            (1..width).forEach { this[it] = mutableListOf() }
        }
    }

    fun start() {
        var state: FunStates? = FunStates.InitState
        loop@ while (true) {
            state = state?.invoke(this)
            if (state == null) break@loop
        }
    }

    fun putSign(column: Int, sign: Char) {
        board[column]?.add(sign)
    }

    fun printBoard() {
        val header = (1..width).joinToString(" ", " ", " ")
        val footer = List(width) { "═" }.joinToString("╩", "╚", "╝")
        val body = List(height) { h ->
            List(width) { w ->
                board[w + 1]?.getOrElse(height - h - 1) { " " }
            }.joinToString("║", "║", "║")
        }.joinToString("\n")

        println(header)
        println(body)
        println(footer)
    }
}

/* TODO: introduce states to OneGame and GameSession
    reduce code repeats
 */
fun interface FunStates {
    operator fun invoke(game: Game): FunStates?

    companion object {
        private val FunStatesRun: (Game.() -> FunStates) -> FunStates =
            { fs ->
                FunStates { it.fs() }
            }

        val InitState = FunStates { Greetings }

        // TODO merge sequential states
        private val Greetings = FunStates {
            println("Connect Four")
            TakeFirstPlayerName
        }

        private val TakeFirstPlayerName = FunStatesRun {
            println("First player's name:")
            firstPlayerName = readLine()!!
            TakeSecondPlayerName
        }

        private val TakeSecondPlayerName = FunStatesRun {
            println("Second player's name:")
            secondPlayerName = readLine()!!
            OfferToChoseBoardSize
        }

        private val OfferToChoseBoardSize = FunStatesRun {
            println(
                "Set the board dimensions (Rows x Columns)\n" +
                        "Press Enter for default (6 x 7)"
            )
            val res = readLine()!!.trim()
            when {
                res == "" -> DefaultSize
                !("[0-9]+\\s*[xX]\\s*[0-9]+".toRegex().matches(res))
                -> InvalidInput
                else -> {
                    val arr = res.split("[xX]".toRegex())
                        .map { it1 -> it1.trim().toInt() }
                    height = arr[0]
                    width = arr[1]
                    when {
                        height !in 5..9 -> IncorrectRowSize
                        width !in 5..9 -> IncorrectColumnSize
                        else -> CorrectSize
                    }
                }
            }
        }

        private val DefaultSize = FunStatesRun {
            height = 6
            width = 7
            OfferToChooseMultipleGames
        }

        private val CorrectSize = FunStates { OfferToChooseMultipleGames }

        private val IncorrectRowSize: FunStates = FunStates {
            println("Board rows should be from 5 to 9")
            OfferToChoseBoardSize
        }

        private val IncorrectColumnSize: FunStates = FunStates {
            println("Board columns should be from 5 to 9")
            OfferToChoseBoardSize
        }

        private val InvalidInput: FunStates = FunStates {
            println("Invalid input")
            OfferToChoseBoardSize
        }

        private val OfferToChooseMultipleGames = FunStatesRun {
            var answer: Int?
            while (true) {
                println(
                    "Do you want to play single or multiple games?\n" +
                            "For a single game, input 1 or press Enter\n" +
                            "Input a number of games:"
                )
                val line = readLine()!!
                answer = line.toIntOrNull()
                if (line.isBlank()) answer = 1
                if (answer != null && answer > 0) {
                    break
                }
                println("Invalid input")
            }
            totalGames = answer!!
            Announcement
        }

        private val Announcement = FunStatesRun {
            println(
                "$firstPlayerName VS $secondPlayerName\n" +
                        "$height X $width board"
            )
            if (totalGames != 1) {
                println("Total $totalGames games")
            }
            StartOfGame
        }
        private val StartOfGame = FunStatesRun {
            initBoard()
            if (totalGames == 1) {
                println("Single game")
            } else {
                println("Game #$currentGame")
            }
            printBoard()
            AskFirstPlayer
        }

        private val AskFirstPlayer = FunStatesRun {
            println("$firstPlayerName's turn:")
            FirstPlayerTurnGet
        }

        // TODO copy-pasted code parametrization
        private val FirstPlayerTurnGet: FunStates = FunStatesRun {
            val line = readLine()!!.trim()
            when (val result = Validation(this, line)) {
                is Validation.Error -> {
                    println(result.text)
                    AskFirstPlayer
                }
                is Validation.Ok -> {
                    if (currentGame % 2 != 0)
                        putSign(result.column, 'o')
                    else
                        putSign(result.column, '*')
                    printBoard()
                    when (val r = CheckWin(this, result.column)) {
                        is CheckWin.Draw -> {
                            firstPlayerScore += 1
                            secondPlayerScore += 1
                            println("It is a draw")
                            EndOfGame
                        }
                        is CheckWin.Win -> {
                            firstPlayerScore += 2
                            println(r.text(firstPlayerName))
                            EndOfGame
                        }
                        CheckWin.NotEnd
                        -> AskSecondPlayer
                    }
                }
                is Validation.End -> EndOfGame
            }
        }
        private val AskSecondPlayer = FunStatesRun {
            println("$secondPlayerName's turn:")
            SecondPlayerTurnGet
        }

        private val SecondPlayerTurnGet: FunStates = FunStatesRun {
            val line = readLine()!!.trim()
            when (val result = Validation(this, line)) {
                is Validation.Error -> {
                    println(result.text)
                    AskSecondPlayer
                }
                is Validation.Ok -> {
                    if (currentGame % 2 != 0)
                        putSign(result.column, '*')
                    else
                        putSign(result.column, 'o')

                    printBoard()
                    when (val r = CheckWin(this, result.column)) {
                        is CheckWin.Draw -> {
                            println("It is a draw")
                            firstPlayerScore += 1
                            secondPlayerScore += 1
                            EndOfGame
                        }
                        is CheckWin.Win -> {
                            secondPlayerScore += 2
                            println(r.text(secondPlayerName))
                            EndOfGame
                        }
                        CheckWin.NotEnd
                        -> AskFirstPlayer
                    }
                }
                is Validation.End -> EndOfGame
            }
        }

        // TODO extract logic from state
        private val EndOfGame = FunStatesRun {
            if (totalGames == 1)
                return@FunStatesRun End


            if (currentGame % 2 == 1)
                println(
                    "Score\n" +
                            "$firstPlayerName: $firstPlayerScore " +
                            "$secondPlayerName: $secondPlayerScore"
                )
            else println(
                "Score\n" +
                        "$secondPlayerName: $secondPlayerScore " +
                        "$firstPlayerName: $firstPlayerScore"
            )

            if (totalGames == currentGame) {
                return@FunStatesRun End
            }
            currentGame += 1
            val tmp = firstPlayerName
            firstPlayerName = secondPlayerName
            secondPlayerName = tmp

            val tmp1 = firstPlayerScore
            firstPlayerScore = secondPlayerScore
            secondPlayerScore = tmp1

            StartOfGame
        }

        private val End = FunStates {
            println("Game over!")
            null
        }
    }
}

sealed class CheckWin {
    companion object {
        operator fun invoke(game: Game, column: Int): CheckWin =
            game.board.run {
                when {
                    Point(column, game.board[column]!!.lastIndex).haveFour(game)
                    -> Win
                    this.values.fold(true)
                    { acc, list -> acc && list.size == game.height }
                    -> Draw("It is a draw")
                    else -> NotEnd
                }
            }
    }

    object Win : CheckWin() {
        fun text(name: String): String = "Player $name won"
    }

    data class Draw(val text: String) : CheckWin()
    object NotEnd : CheckWin()
}


sealed class Validation {
    companion object {
        operator fun invoke(game: Game, suspect: String)
                : Validation =
            game.run {
                val res = suspect.toIntOrNull()
                when {
                    suspect == "end"
                    -> End

                    res == null
                    -> Error("Incorrect column number")

                    res !in 1..(width)
                    -> Error("The column number is out of range (1 - $width)")

                    board[res]?.size == height
                    -> Error("Column $res is full")

                    else -> Ok(res)
                }
            }
    }

    data class Ok(val column: Int) : Validation()
    data class Error(val text: String) : Validation()
    object End : Validation()
}

data class Point(val x: Int, val y: Int) {
    fun getNear(game: Game): List<Point> {
        val l = List(3) { (x - 1)..(x + 1) }.flatten()
            .zip(((y - 1)..(y + 1)).flatMap { listOf(it, it, it) })
            .filter { it.first != x || it.second != y }
            .map { Point(it.first, it.second) }
            .filter { it.isOnBoard(game) }
            .toList()
        return l
    }

    fun isOnBoard(game: Game) = game.board[x]?.getOrNull(y) != null

    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)

    fun getListOfFours(game: Game): List<List<Point>> {
        val l = getNear(game).map {
            val vector = it - this
            val f = it + vector
            val s = f + vector
            listOf(this, it, f, s)
        }
        return l
    }

    fun haveFour(game: Game): Boolean = !getListOfFours(game)
        .filter { it.filter { it2 -> it2.isOnBoard(game) }.size == 4 }
        .map { it.map { (x1, y1) -> game.board[x1]?.get(y1) } }
        .none { it.reduce { acc, c -> if (acc == c) acc else null } != null }
}


fun main() {
    Game().start()
}

