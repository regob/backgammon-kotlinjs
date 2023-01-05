import kotlin.js.Date
import kotlin.test.*

class TestWorker {
    @Test
    fun testHeuristicScorer() {
        val scorer = HeuristicScorer()
        for (i in 0..3)
            for (j in 0..scorer.MX)
                assertEquals(1.0, scorer.winProb[i][j], 1e6)

        for ((i, j) in listOf(50 to 13, 220 to 220, 190 to 175, 110 to 126, 50 to 46, 60 to 65, 49 to 65)) {
            println("P[$i, $j] = ${scorer.winProb[i][j]}")
        }

    }

    @Test
    fun testMinimax() {
        val state = initialGameState(1)
        state.setDice(Dice(4, 5))

        val root = MiniMaxNode(true)
        val startDate = Date.now()
        root.expand(state, 1, 1, Date.now() + 500)
        assertNotNull(root.value, "Root value should not be null")
        println("Expansion took ${Date.now() - startDate} ms. Root value: ${root.value}")
        assertNotNull(root.children, "Root children should not be null")
        assertNotNull(root.actionPerChildren, "Root actionsPerChildren should not be null")
        assertEquals(10, root.children!!.size, "Root should have 10 children")
        assertEquals(10, root.actionPerChildren!!.size, "Root should have 10 children")
        for (node in root.children!!) {
            assertFalse(node.isTerminal, "Child of root should not be terminal")
            assertNull(node.children, "Child of root should not have children (depth=1)")
            assertNotNull(node.value, "Child of root should have non-null value")
        }
    }
}