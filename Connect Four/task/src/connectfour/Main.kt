package connectfour

import kotlin.properties.Delegates

sealed class Player {
    lateinit var name: String
    var score: Int = 0

    object One : Player()
    object Two : Player()

    operator fun inc(): Player {
        score++
        return this
    }

    operator fun not(): Player = when (this) {
        is One -> Two
        is Two -> One
    }

    operator fun component1() = name
    operator fun component2() = score
}
typealias RawBoard = Map<Int, MutableList<Char>>

data class Board(val width: Int, val height: Int) {
    var raw: RawBoard = HashMap<Int, MutableList<Char>>().apply {
        (1..width).forEach { this[it] = mutableListOf() }
    }

    fun print() {
        val header = (1..width).joinToString(" ", " ", " ")
        val footer = List(width) { "═" }.joinToString("╩", "╚", "╝")
        val body = List(height) { h ->
            List(width) { w ->
                raw[w + 1]?.getOrElse(height - h - 1) { " " }
            }.joinToString("║", "║", "║")
        }.joinToString("\n")

        println(header)
        println(body)
        println(footer)
    }

    fun putSign(column: Int, sign: Char) {
        raw[column]?.add(sign)
    }
}

class OneGame(starterPlayer: Player, width: Int, height: Int) {
    /* TODO: data class Player(score)
        Player1, Player2
    */

    private var currPlayer = starterPlayer
    private var board: Board = Board(width, height)

    init {
        board.print()
        AskCurrPlayer()
    }

    var firstScore: Int = 0
    var secondScore: Int = 0


    fun AskCurrPlayer() {
        println("${currPlayer.name}'s turn:")
        CurrPlayerTurnGet()
    }

    fun CurrPlayerTurnGet() {
        val line = readLine()!!.trim()
        when (val result = Validation(board, line)) {
            is Validation.Error -> {
                println(result.text)
                AskCurrPlayer()
            }
            is Validation.Ok -> {
                val sig = when (currPlayer) {
                    Player.One -> 'o'
                    Player.Two -> '*'
                }
                board.putSign(result.column, sig)
                board.print()
                when (val r = CheckWin(board, result.column)) {
                    is CheckWin.Draw -> {
                        currPlayer++
                        currPlayer = !currPlayer
                        currPlayer++
                        currPlayer = !currPlayer
                        println("It is a draw")
                        EndOfGame()
                    }
                    is CheckWin.Win -> {
                        currPlayer++
                        currPlayer++
                        println(r.text(currPlayer.name))
                        EndOfGame()
                    }
                    CheckWin.NotEnd
                    -> {
                        currPlayer = !currPlayer
                        AskCurrPlayer()
                    }
                }
            }
            is Validation.End -> EndOfGame()
        }
    }

    fun EndOfGame() {

    }
}

class GameSession {
    var currPlayer: Player = Player.One
    var totalGames = 1
    var currentGame = 1

    var height by Delegates.notNull<Int>()
    var width by Delegates.notNull<Int>()


    fun start() {
        Greetings()
    }

    fun Greetings() {
        println("Connect Four")
        println("First player's name:")
        Player.One.name = readLine()!!
        println("Second player's name:")
        Player.Two.name = readLine()!!
        OfferToChoseBoardSize()
    }

    fun OfferToChoseBoardSize() {
        println(
            "Set the board dimensions (Rows x Columns)\n" +
                    "Press Enter for default (6 x 7)"
        )
        val res = readLine()!!.trim()
        when {
            res == "" -> DefaultSize()
            !("[0-9]+\\s*[xX]\\s*[0-9]+".toRegex().matches(res))
            -> InvalidInput()
            else -> {
                val arr = res.split("[xX]".toRegex())
                    .map { it1 -> it1.trim().toInt() }
                height = arr[0]
                width = arr[1]
                when {
                    height !in 5..9 -> IncorrectRowSize()
                    width !in 5..9 -> IncorrectColumnSize()
                    else -> CorrectSize()
                }
            }
        }
    }

    fun DefaultSize() {
        height = 6
        width = 7
        OfferToChooseMultipleGames()
    }

    fun CorrectSize() {
        OfferToChooseMultipleGames()
    }

    fun IncorrectRowSize() {
        println("Board rows should be from 5 to 9")
        OfferToChoseBoardSize()
    }

    fun IncorrectColumnSize() {
        println("Board columns should be from 5 to 9")
        OfferToChoseBoardSize()
    }

    fun InvalidInput() {
        println("Invalid input")
        OfferToChoseBoardSize()
    }

    fun OfferToChooseMultipleGames() {
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
        Announcement()
    }

    fun Announcement() {
        println(
            "${Player.One.name} VS ${Player.Two.name}\n$height X $width board"
        )
        if (totalGames != 1) {
            println("Total $totalGames games")
        }
        StartOfGame()
    }

    fun StartOfGame() {
        if (totalGames == 1) {
            println("Single game")
        } else {
            println("Game #$currentGame")
        }
        OneGame(currPlayer, width, height)
        // TODO start game, take result
        EndOfGame()
    }


    fun EndOfGame() {
        if (totalGames == 1)
            return End()

        val (cName, cScore) = Player.One
        val (oName, oScore) = Player.Two

        println("Score\n$cName: $cScore $oName: $oScore")

        if (totalGames == currentGame)
            return End()

        currentGame += 1

        currPlayer = !currPlayer
        StartOfGame()
    }

    fun End() {
        println("Game over!")
        null
    }
}

sealed class CheckWin {
    companion object {
        operator fun invoke(board: Board, column: Int):
                CheckWin = board.run {
            when {
                Point(
                    column,
                    raw[column]!!.lastIndex
                ).haveFour(raw)
                -> Win
                this.raw.values.fold(true)
                { acc, list -> acc && list.size == height }
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
        operator fun invoke(board: Board, suspect: String): Validation {
            val res = suspect.toIntOrNull()
            return when {
                suspect == "end"
                -> End

                res == null
                -> Error("Incorrect column number")

                res !in 1..(board.width)
                -> Error("The column number is out of range (1 - ${board.width})")

                board.raw[res]?.size == board.height
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
    fun getNear(rawBoard: RawBoard): List<Point> {
        val l = List(3) { (x - 1)..(x + 1) }.flatten()
            .zip(((y - 1)..(y + 1)).flatMap { listOf(it, it, it) })
            .filter { it.first != x || it.second != y }
            .map { Point(it.first, it.second) }
            .filter { it.isOnBoard(rawBoard) }
            .toList()
        return l
    }

    fun isOnBoard(rawBoard: RawBoard) =
        rawBoard[x]?.getOrNull(y) != null

    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)

    fun getListOfFours(rawBoard: RawBoard): List<List<Point>> {
        val l = getNear(rawBoard).map {
            val vector = it - this
            val f = it + vector
            val s = f + vector
            listOf(this, it, f, s)
        }
        return l
    }

    fun haveFour(rawBoard: RawBoard): Boolean =
        !getListOfFours(rawBoard)
            .filter { it.filter { it2 -> it2.isOnBoard(rawBoard) }.size == 4 }
            .map { it.map { (x1, y1) -> rawBoard[x1]?.get(y1) } }
            .none { it.reduce { acc, c -> if (acc == c) acc else null } != null }
}


fun main() {
    GameSession().start()
}

