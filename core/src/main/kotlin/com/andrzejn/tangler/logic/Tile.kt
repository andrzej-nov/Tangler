package com.andrzejn.tangler.logic

import kotlin.random.Random

/**
 * Tile contains one or several colored segments. Until put to a field cell, the tile can be rotated. After putting
 * to the cell the tile is not moved anymore. It is cleared and destroyed when all of its segments are cleared
 * as parts of the closed path loops.
 */
class Tile(private val sidesCount: Int, private val colorsCount: Int, private val allowDuplicateColors: Boolean) {
    private val halfSidesCount = sidesCount / 2

    /**
     * The tile segments. Once created, they aren't rearranged or added. Segments are removed when respective paths
     * are cleared. When tile is rotated, segment end sides are updated, but the segment list is not rearranged.
     */
    val segment: MutableList<Segment> = mutableListOf()

    /**
     * The cell to which the tile is put on. Until put to a cell, it is null
     */
    var cell: Cell? = null

    /**
     * Colors of the tile sides. Order is the same as for the cell borders (starting from the top-left side, clockwise).
     * That is a helper array created and updated from the segment ends and colors.
     * Empty side (without a cell) has color 0.
     */
    val tileColors: Array<Int> = Array(sidesCount) { 0 }

    /**
     * Clone the tile with its segments. Used to create a tile copy to rotate and evaluate the best move.
     */
    fun clone(): Tile {
        return Tile(sidesCount, colorsCount, allowDuplicateColors).also { t ->
            this.segment.forEach { t.segment.add(it.cloneTo(t)) }
            rebuildTileColors()
        }
    }

    /**
     * Returns the random number of tile segments to generate when creating a new random tile.
     * Values in those lambda blocks determine probabilities to encounter respective segment counts.
     * It is defined as several lambdas instead of normal method because I was interested to try that approach :)
     * and to save several lines on the loop copies
     */
    val randomNumberOfSegments: () -> Int = when (halfSidesCount) {
        2 -> { -> if (Random.nextDouble() < 0.25) 1 else 2 }
        3 -> { ->
            val r = Random.nextDouble()
            if (r < 0.15) 1 else if (r < 0.4) 2 else 3
        }
        else -> { ->
            val r = Random.nextDouble()
            if (r < 0.15) 1 else if (r < 0.35) 2 else if (r < 0.6) 3 else 4
        }
    }

    /**
     * Generate and add to the tile random number of random segments, taking into account the game settings.
     */
    fun createRandomSegments(segmentsCount: Int = randomNumberOfSegments()) {
        val sides = (0 until sidesCount).shuffled()
        // The sides list contains randomized sequence of cell sides, to use in the generated segments
        val colors = (if (allowDuplicateColors) (1..colorsCount) else emptyList()).plus((1..colorsCount)).shuffled()
        // The colors list contains randomized sequence of all available colors, to use in the generated segments
        var j = 0
        for (i in 0 until segmentsCount) {
            segment.add(Segment(colors[i], sides[j], sides[j + 1], this))
            j += 2
        }
        rebuildTileColors()
    }

    /**
     * Update the tileColors array according to the current segments state
     */
    private fun rebuildTileColors() {
        tileColors.fill(0)
        segment.forEach { s ->
            s.endsAtSide.forEach { tileColors[it] = s.color }
        }
    }

    /**
     * Rotate the tile (by updating the segment ends respectively).
     * For sidesCount=8 specify steps in (0, 2, 4, 6) range only, as that is actually
     * a square tile with corner connections
     */
    fun rotateBy(steps: Int) {
        if (steps == 0)
            return
        segment.forEach { s ->
            (0..1).forEach { i ->
                s.endsAtSide[i] = addClipWrapSteps(s.endsAtSide[i], steps)
            }
        }
        rebuildTileColors()
    }

    /**
     * Add rotation steps, clipping result to the (0..sidesCount-1) range and wrapping from another side as needed
     */
    private fun addClipWrapSteps(value: Int, steps: Int): Int {
        val n = value + steps
        return when {
            n >= sidesCount -> n - sidesCount
            n < 0 -> n + sidesCount
            else -> n
        }
    }

    /**
     * Remove segment from the tile. Clear tile from the cell if that was the last segment.
     */
    fun remove(s: Segment) {
        segment.remove(s)
        rebuildTileColors()
        if (segment.isEmpty()) {
            cell?.tile = null
            cell = null
        }
    }

    /**
     * Serialize the tile as part of the game save
     */
    fun serialize(sb: com.badlogic.gdx.utils.StringBuilder) {
        sb.append(segment.size)
        segment.forEach { it.serialize(sb) }
    }

    /**
     * Deserialize the tile segments on game load
     */
    fun deserialize(s: String, i: Int): Int {
        val cnt = s[i].digitToInt()
        if (cnt == 0 || cnt > halfSidesCount)
            return -1
        segment.clear()
        var j = i + 1
        repeat(cnt) {
            segment.add(Segment(s[j].digitToInt(), s[j + 1].digitToInt(), s[j + 2].digitToInt(), this))
            j += 3
        }
        rebuildTileColors()
        return j
    }

    /**
     * Given the desired sides color sequence, generate the tile segments that match that sequence as close as possible
     * (used to generate a tile that should fit into particular field cell during the game)
     */
    fun createSegmentsForColors(borderColors: Array<PlayField.BorderColor>) {
        while (true) { // Connect provided colors with segments, using available empty sides as needed.
            val segmentStart = indexOfRandomOrNull(borderColors) { (border, _) -> border.color > 0 } ?: break
            val segmentColor = borderColors[segmentStart].color
            borderColors[segmentStart].color = -1
            val segmentEnd = indexOfRandomOrNull(borderColors) { (border, _) -> border.color == segmentColor }
                ?: indexOfRandomOrNull(borderColors) { (bc, _) ->
                    bc.color == 0 && bc.nonBlockingOptions.contains(segmentColor)
                }
                ?: continue
            borderColors[segmentEnd].color = -1
            segment.add(Segment(segmentColor, segmentStart, segmentEnd, this))
            if (!allowDuplicateColors)
                borderColors.forEachIndexed { i, (color, _) ->
                    borderColors[i].nonBlockingOptions.remove(segmentColor)
                    if (color == segmentColor) borderColors[i].color = -1
                }
        }
        if (segment.size < halfSidesCount) { // Try adding one more random segment
            val segmentStart =
                indexOfRandomOrNull(borderColors) { (bc, _) -> bc.color == 0 && bc.nonBlockingOptions.size > 0 }
            if (segmentStart != null) {
                val segmentColor = borderColors[segmentStart].nonBlockingOptions.random()
                borderColors[segmentStart].nonBlockingOptions.remove(segmentColor)
                val segmentEnd = indexOfRandomOrNull(borderColors) { (bc, _) ->
                    bc.color == 0 && bc.nonBlockingOptions.contains(segmentColor)
                }
                if (segmentEnd != null)
                    segment.add(Segment(segmentColor, segmentStart, segmentEnd, this))
            }
        }
        var rotateSteps = Random.nextInt(sidesCount) // Randomly rotate generated tile to make it not that obvious
        if (sidesCount == 8) rotateSteps -= rotateSteps % 2
        if (rotateSteps != 0) rotateBy(rotateSteps)
        rebuildTileColors()
    }

    /**
     * Returns index of random array element that matches given selector filter.
     */
    private fun indexOfRandomOrNull(
        borderColors: Array<PlayField.BorderColor>,
        filterBy: (Pair<PlayField.BorderColor, Int>) -> Boolean
    ) =
        borderColors.mapIndexed { i, bc -> bc to i }.filter(filterBy).randomOrNull()?.second

    /**
     * Copies segments from other tile. Used to save/restore last move.
     */
    fun cloneFrom(other: Tile) {
        segment.clear()
        other.segment.forEach {
            segment.add(Segment(it.color, it.endsAtSide[0], it.endsAtSide[1], this))
        }
        rebuildTileColors()
    }
}
