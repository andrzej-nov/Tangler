package com.andrzejn.tangler.tiles

import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Segment
import com.andrzejn.tangler.logic.Tile
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.FrameBuffer

/**
 * Base class of the screen tile (contains colored segments, can be rotated then put to a board cell).
 * Provides common properties and methods, independent from the tile sides count.
 */
abstract class BaseTile(
    /**
     * The logical tile providing segments and cell values
     */
    val t: Tile,
    /**
     * Reference to the app Context object
     */
    protected val ctx: Context
) {
    /**
     * The sprite generated for this tile. It could be then drawn on any part of the board.
     * Regenerated when the screen resizes or tile rotates.
     */
    var sprite: Sprite = Sprite()

    /**
     * If the sprite object is valid (or, if false, if it should be regenerated before drawing)
     */
    var isSpriteValid: Boolean = false

    /**
     * OpenGL frame buffer used to generate the sprite. Each tile has its own small frame buffer, kept alive and actual,
     * because sprite does not store its own copy of the texture and just references the framebuffer texture.
     */
    private var fbo = FrameBuffer(Pixmap.Format.RGBA8888, 10, 10, false, false)

    /**
     * X coordinate of the board cell where the tile has been put to. A copy of the cell field is stored here
     * because sometimes we need to draw the tile animations when respective logical tile is not yet attached
     * to the cell or is already cleared from the cell
     */
    var x: Int = -1

    /**
     * Y coordinate of the board cell where the tile has been put to. A copy of the cell field is stored here
     * because sometimes we need to draw the tile animations when respective logical tile is not yet attached
     * to the cell or is already cleared from the cell
     */
    var y: Int = -1

    init {
        x = t.cell?.x ?: -1
        y = t.cell?.y ?: -1
    }

    /**
     * Regenerate the frame buffer when the sprite is resized during screen window resize
     */
    fun setSpriteSize(width: Int, height: Int) {
        if (fbo.width == width && fbo.height == height)
            return
        isSpriteValid = false
        fbo.dispose()
        fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false, false)
    }

    /**
     * The current tile background polygon (a copy of respective cell polygon)
     */
    protected abstract val tilePolygon: FloatArray

    private var inBuildSprite = false

    /**
     * Generate the tile sprite by drawing segments on the framebuffer. Then the sprite is just rendered on screen
     * as needed without all that complicated drawing.
     */
    fun buildSprite() {
        if (inBuildSprite)
            return
        inBuildSprite = true
        fbo.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f) // Set whole rectangle to fully transparent black
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        ctx.tileBatch.begin() // tileBatch has dedicated camera with the viewport adjusted to the current tile size.
        // Note: only one batch may be active at the time, so the tile sprites must be generated outside of the
        // main screen render method
        ctx.drw.tsd.setColor(ctx.drw.theme.screenBackground)
        ctx.drw.tsd.filledPolygon(tilePolygon)
        t.segment.forEach {
            drawSegment(it)
        }
        ctx.tileBatch.end()
        fbo.end()
        val (x, y) = sprite.x to sprite.y
        sprite = Sprite(fbo.colorBufferTexture).apply {
            setOriginCenter()
            setPosition(x, y)
        }
        isSpriteValid = true
        inBuildSprite = false
    }

    /**
     * Draws a single segment. A lot of specific case details implemented in the subclass classes
     */
    protected abstract fun drawSegment(segment: Segment)

    /**
     * Returns the segment color. When the segment is a part of disappearing closed loop, its color alpha channel
     * is controlled by the Tween Engine.
     */
    protected fun segmentColor(s: Segment, c: Color): Color {
        if (ctx.fader.inFade && ctx.fader.affected(s))
            return Color(1f, 1f, 1f, ctx.fader.alpha)
        return c
    }

    /**
     * Dispose the frame buffer (not the whole tile object, just its frame buffer; the buffer can be recreated again
     * later)
     */
    fun disposeFrameBuffer() {
        fbo.dispose()
    }

    /**
     * Rotates the base logic tile segments by given steps (that depends on the cell sides count),
     * then invalidates the sprite so it will be regenerated
     */
    open fun rotateBy(steps: Int) {
        // Override this function and rotate logical tile
        isSpriteValid = false
    }

}