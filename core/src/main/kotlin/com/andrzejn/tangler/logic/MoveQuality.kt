package com.andrzejn.tangler.logic

/**
 * Stores the parameters that affect the possible move quality, and provides the comparator to find the best moves
 */
class MoveQuality : Comparable<MoveQuality> {
    // Negative factor weight, from worst to better:

    /**
     * Number of adjacent cells blocked by that move
     */
    var cellsBlocked: Int = 0

    /**
     * Number of adjacent paths blocked (broken) by that move
     */
    var pathsBlocked: Int = 0

    // Positive factor weight, from best to less important:

    /**
     * Number of closed path loops created by that move
     */
    var loopsClosed: Int = 0

    /**
     * Number of adjacent paths extended by that move (not including closed loops)
     */
    var pathsExtended: Int = 0

    /**
     * Total length (sum of segments count) of the closed path loops
     */
    var closedLoopsTotalLength: Int = 0

    /**
     * Total length (sum of segments count) of the closed path loops (not including closed loops length)
     */
    var extendedPathTotalLength: Int = 0

    /**
     * The comparator that allows methods like maxBy or sortedBy properly work with MoveQuality objects
     */
    override fun compareTo(other: MoveQuality): Int = compareValuesBy(this, other,
        { -it.cellsBlocked },
        { -it.pathsBlocked },
        { it.loopsClosed },
        { it.pathsExtended },
        { it.closedLoopsTotalLength },
        { it.extendedPathTotalLength })
}
