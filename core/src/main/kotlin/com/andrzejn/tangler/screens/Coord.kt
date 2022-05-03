package com.andrzejn.tangler.screens

import com.andrzejn.tangler.logic.Cell
import com.andrzejn.tangler.tiles.BaseTile

/**
 * Magical number meaning the coord is not set
 */
const val unsetCoord: Int = -999

/**
 * Playfield cell indices
 */
class Coord(
    /**
     * X coord. Normally in range (0 until boardSize)
     */
    var x: Int = unsetCoord,
    /**
     * Y coord. Normally in range (0 until boardSize)
     */
    var y: Int = unsetCoord,
) {
    constructor(c: Coord) : this(c.x, c.y)
    constructor(cell: Cell) : this(cell.x, cell.y)

    /**
     * Set this coord equal to the one passed as the parameter. Returns this instance to allow chained calls.
     */
    fun set(fromOther: Coord): Coord {
        x = fromOther.x
        y = fromOther.y
        return this
    }

    /**
     * Set this coord to the cell coords. Returns this instance to allow chained calls.
     */
    fun set(from: Cell): Coord {
        x = from.x
        y = from.y
        return this
    }

    /**
     * Set this coord to the tile coords. Returns this instance to allow chained calls.
     */
    fun set(from: BaseTile): Coord {
        x = from.x
        y = from.y
        return this
    }

    /**
     * Set this coord to the given values. Returns this instance to allow chained calls.
     */
    fun set(x: Int, y: Int): Coord {
        this.x = x
        this.y = y
        return this
    }

    /**
     * Add this coord to the given values. Returns this instance to allow chained calls.
     */
    fun add(x: Int, y: Int): Coord {
        this.x += x
        this.y += y
        return this
    }

    /**
     * Standard equality check
     */
    override fun equals(other: Any?): Boolean = (other is Coord) && this.x == other.x && this.y == other.y

    /**
     * True if at least one of the coordinates is not 0
     */
    fun isNotZero(): Boolean {
        return x != 0 || y != 0
    }

    /**
     * True if both coordinates are not unsetCoord
     */
    fun isSet(): Boolean {
        return x != unsetCoord && y != unsetCoord
    }

    /**
     * True if both coordinates are unsetCoord
     */
    fun isNotSet(): Boolean {
        return x == unsetCoord && y == unsetCoord
    }

    /**
     * Unset this instance
     */
    fun unSet(): Coord {
        x = unsetCoord
        y = unsetCoord
        return this
    }

    /**
     * Class hashcode implementation
     */
    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

}