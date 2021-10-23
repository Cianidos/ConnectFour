package connectfour

// FIXME I am to large and complicated
class PlayersPair(nameOne: String, nameTwo: String) {
    interface IPlayer {
        val name: String
        val score: Int
        val sign: Char
        operator fun not(): IPlayer
        operator fun component1() = name
        operator fun component2() = score
    }

    // FIXME I am not need to contain score
    inner class Player(override val name: String, override val sign: Char) :
        IPlayer {
        override var score: Int = 0
            private set

        override operator fun not(): IPlayer = when (this) {
            one -> two
            two -> one
            else -> throw IllegalArgumentException("Impossible")
        }

        // FIXME win/draw logic duplication
        fun win() {
            score += 2
        }

        // FIXME win/draw logic duplication
        fun draw() {
            _one.score += 1
            _two.score += 1
        }
    }

    // FIXME win/draw logic duplication
    fun draw() = _one.draw()

    // FIXME win/draw logic duplication
    // FIXME associative logic implemented with if
    fun win(player: IPlayer) = when (player.name) {
        one.name -> _one.win()
        two.name -> _two.win()
        else -> throw IllegalArgumentException()
    }

    fun produceEmpty(): PlayersPair {
        return PlayersPair(one.name, two.name)
    }

    fun swapPlayers() {
        val tmp = _one
        _one = _two
        _two = tmp
    }

    private var _one: Player = Player(nameOne, 'o')
    val one: IPlayer
        get() = _one

    private var _two: Player = Player(nameTwo, '*')
    val two: IPlayer
        get() = _two
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

class OneGame(players: PlayersPair, width: Int, height: Int) {
    private var board: Board = Board(width, height)

    // FIXME player.score are not needed in this class
    private var currPlayer = players.one

    fun start(): CheckWin.End {
        board.print()
        return currPlayerTurnGet()
    }

    private tailrec fun currPlayerTurnGet(): CheckWin.End {
        println("${currPlayer.name}'s turn:")
        val line = readLine()!!.trim()
        return when (val result = Validation(board, line)) {
            is Validation.Error -> {
                println(result.text)
                currPlayerTurnGet()
            }
            is Validation.End -> CheckWin.InputEnd
            is Validation.ValidMove -> {
                val sig = currPlayer.sign
                board.putSign(result.column, sig)
                board.print()
                when (val r = CheckWin(board, result.column, currPlayer)) {
                    is CheckWin.End -> r
                    CheckWin.NotEnd -> {
                        currPlayer = !currPlayer
                        currPlayerTurnGet()
                    }
                }
            }
        }
    }
}

class GameSession {
    private lateinit var players: PlayersPair
    private var totalGames = 1
    private var currentGame = 1

    private var height = 6
    private var width = 7

    private fun isGameOdd() = currentGame % 2 == 0

    fun start() {
        greetings()
    }

    private fun greetings() {
        println("Connect Four")
        println("First player's name:")
        val nameOne = (readLine() ?: return)
        println("Second player's name:")
        val nameTwo = (readLine() ?: return)
        players = PlayersPair(nameOne, nameTwo)

        offerToChoseBoardSize()
    }

    private tailrec fun offerToChoseBoardSize() {
        println(
            """
                Set the board dimensions (Rows x Columns)
                Press Enter for default (6 x 7)
                """.trimIndent()
        )
        val res = (readLine() ?: return).trim()
        when {
            res == "" -> offerToChooseMultipleGames()
            !("[0-9]+\\s*[xX]\\s*[0-9]+".toRegex().matches(res))
            -> {
                println("Invalid input")
                offerToChoseBoardSize()
            }
            else -> {
                val arr = res.split("[xX]".toRegex())
                    .map { it1 -> it1.trim().toInt() }
                height = arr[0]
                width = arr[1]
                when {
                    height !in 5..9 -> {
                        println("Board rows should be from 5 to 9")
                        offerToChoseBoardSize()
                    }

                    width !in 5..9 -> {
                        println("Board columns should be from 5 to 9")
                        offerToChoseBoardSize()
                    }
                    else -> offerToChooseMultipleGames()
                }
            }
        }
    }

    private fun offerToChooseMultipleGames() {
        var answer: Int?
        while (true) {
            println(
                "Do you want to play single or multiple games?\n" +
                        "For a single game, input 1 or press Enter\n" +
                        "Input a number of games:"
            )
            val line = readLine() ?: return
            answer = line.toIntOrNull()
            if (line.isBlank()) answer = 1
            if (answer != null && answer > 0) {
                break
            }
            println("Invalid input")
        }
        totalGames = (answer ?: return)
        announcement()
    }

    private fun announcement() {
        println(
            "${players.one.name} VS ${players.two.name}\n$height X $width " +
                    "board"
        )
        if (totalGames != 1) {
            println("Total $totalGames games")
        }
        startOfGame()
    }

    private fun startOfGame() {
        if (totalGames == 1) {
            println("Single game")
        } else {
            println("Game #$currentGame")
        }
        endOfGame(
            OneGame(
                players.produceEmpty().apply { if (isGameOdd()) swapPlayers() },
                width, height
            ).start()
        )
    }


    // TODO: I think it is too large and strange
    private fun endOfGame(end: CheckWin.End) {
        when (end) {
            is CheckWin.Draw -> {
                players.draw()
                println("It is a draw")
            }
            is CheckWin.Win -> {
                players.win(end.player)
                println(end.text())
            }
            is CheckWin.InputEnd -> return end()
        }

        if (totalGames == 1) return end()

        val (cName, cScore) = players.one
        val (oName, oScore) = players.two

        println("Score\n$cName: $cScore $oName: $oScore")

        if (totalGames == currentGame) return end()

        currentGame += 1
        startOfGame()
    }

    private fun end() {
        println("Game over!")
    }
}

sealed class CheckWin {
    companion object {
        operator fun invoke(
            board: Board,
            column: Int,
            player: PlayersPair.IPlayer
        ):
                CheckWin = board.run {
            when {
                Point(
                    column,
                    raw[column]!!.lastIndex
                ).haveFour(raw)
                -> Win(player)
                this.raw.values.fold(true)
                { acc, list -> acc && list.size == height }
                -> Draw("It is a draw")
                else -> NotEnd
            }
        }
    }

    sealed class End : CheckWin()

    object InputEnd : End()

    data class Win(val player: PlayersPair.IPlayer) : End() {
        fun text(): String = "Player ${player.name} won"
    }

    data class Draw(val text: String) : End()
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

                else -> ValidMove(res)
            }
        }
    }

    data class ValidMove(val column: Int) : Validation()
    data class Error(val text: String) : Validation()
    object End : Validation()
}

data class Point(val x: Int, val y: Int) {
    private fun getNear(rawBoard: RawBoard): List<Point> =
        List(3) { (x - 1)..(x + 1) }.flatten()
            .zip(((y - 1)..(y + 1)).flatMap { listOf(it, it, it) })
            .filter { it.first != x || it.second != y }
            .map { Point(it.first, it.second) }
            .filter { it.isOnBoard(rawBoard) }
            .toList()

    private fun isOnBoard(rawBoard: RawBoard) =
        rawBoard[x]?.getOrNull(y) != null

    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)

    private fun getListOfFours(rawBoard: RawBoard) =
        getNear(rawBoard).map {
            val vector = it - this
            val f = it + vector
            val s = f + vector
            listOf(this, it, f, s)
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
