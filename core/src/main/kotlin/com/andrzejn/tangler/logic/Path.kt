package com.andrzejn.tangler.logic

/**
 * Path is a continuous chain of segments with same color. Paths grow when new adjacent tiles are put to the field.
 * When a path becomes a closed loop, it is removed and he score increased.
 */
class Path(firstSegment: Segment) {
    /**
     * Cell borders on the ends of the path. No particular order (i.e. there is no difference between the path head
     * and tail)
     */
    private var border = arrayOfNulls<Border>(2)

    /**
     * Cells adjacent to the path begin and end. The neighbourCell order is the same as the border order
     * (e.g. border`[`0`]` belongs to neighbourCell`[`0`]`)
     */
    private var neighbourCell = arrayOfNulls<Cell>(2)

    /**
     * The list of path segments, in sequential order. border`[`0`]` belongs to segment`[`0`]`,
     * border`[`1`]` belongs to the segment.last()
     */
    val segment: MutableList<Segment> = mutableListOf(firstSegment)

    /**
     * The path starts with a single segment. There are single-segment paths, there are no paths without segments.
     */
    init {
        firstSegment.path = this
        border = firstSegment.border.copyOf()
        neighbourCell = firstSegment.neighbourCell.copyOf()
    }

    /**
     * Add segment to the path. The method automatically determines to which path end and with which orientation
     * the segment should be added. (Assuming the segment matches one of the path ends, there are no checks if the
     * segment is wrong, the calling method should take care of that).
     *
     * This method does not check if the segment closes path into a loop, that is done in respective PlayField method.
     */
    fun append(s: Segment) {
        s.path = this
        when (s.cell) {
            neighbourCell[1] -> { // append to the end of the segments list
                segment.add(s)
                when {
                    border[1] == s.border[0] -> {
                        border[1] = s.border[1]
                        neighbourCell[1] = s.neighbourCell[1]
                    }

                    else -> {
                        border[1] = s.border[0]
                        neighbourCell[1] = s.neighbourCell[0]
                    }
                }
            }

            else -> { // insert into the beginning of the segments list
                segment.add(0, s)
                when {
                    border[0] == s.border[0] -> {
                        border[0] = s.border[1]
                        neighbourCell[0] = s.neighbourCell[1]
                    }

                    else -> {
                        border[0] = s.border[0]
                        neighbourCell[0] = s.neighbourCell[0]
                    }
                }
            }
        }
    }

    /**
     * Process the case when segment joins two paths together, creating one long path.
     * No checks are performed, assuming that the segment and otherPath are provided correctly.
     */
    fun mergeWith(s: Segment, otherPath: Path) {
        append(s)
        otherPath.segment.forEach { it.path = this }
        when {
            border[1] == otherPath.border[0] -> {
                segment.addAll(otherPath.segment)
                border[1] = otherPath.border[1]
                neighbourCell[1] = otherPath.neighbourCell[1]
            }

            border[1] == otherPath.border[1] -> {
                segment.addAll(otherPath.segment.reversed())
                border[1] = otherPath.border[0]
                neighbourCell[1] = otherPath.neighbourCell[0]
            }

            border[0] == otherPath.border[1] -> {
                segment.addAll(0, otherPath.segment)
                border[0] = otherPath.border[0]
                neighbourCell[0] = otherPath.neighbourCell[0]
            }

            else -> { // if (border[0] == otherPath.border[0])
                segment.addAll(0, otherPath.segment.reversed())
                border[0] = otherPath.border[1]
                neighbourCell[0] = otherPath.neighbourCell[1]
            }
        }
    }

    /**
     * Clear the path, removing segments from respective tiles (and tiles, in turn, might then clear themselves
     * from the cells if they have no other segments). Invoked when the path becomes a closed loop and is removed.
     */
    fun clear() {
        border.fill(null)
        neighbourCell.fill(null)
        segment.forEach {
            it.tile.remove(it)
            it.border.forEach { b -> b?.clear() }
            it.neighbourCell.fill(null)
        }
    }

}