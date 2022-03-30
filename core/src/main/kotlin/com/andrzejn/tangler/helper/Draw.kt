package com.andrzejn.tangler.helper

import com.andrzejn.tangler.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import space.earlygrey.shapedrawer.ShapeDrawer


/**
 * The drawing helper objects
 */
class Draw(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {
    /**
     * Camera for drawing all screens. Simple unmoving orthographic camera for static 2D view.
     */
    lateinit var camera: OrthographicCamera

    /**
     * Camera for drawing tile sprites. Simple unmoving orthographic camera for static 2D view.
     */
    private lateinit var tileCamera: OrthographicCamera

    /**
     * Drawer for geometric shapes on the screens
     */
    lateinit var sd: ShapeDrawer

    /**
     * Drawer for geometric shapes on the tiles
     */
    lateinit var tsd: ShapeDrawer

    /**
     * Light (bright) colors palette for the tile lines
     */
    val light: Array<Color> = arrayOf(
        Color.WHITE,
        Color(0xffd966ff.toInt()),
        Color(0x6d9eebff),
        Color(0x93c47dff.toInt()),
        Color(0xb250ffff.toInt()),
        Color(0xd68a00ff.toInt()),
        Color(0xcc4125ff.toInt())
    )

    /**
     * Darker colors palette for the tile lines' edges
     */
    val dark: Array<Color> = arrayOf(
        Color.LIGHT_GRAY,
        Color(0xbf9000ff.toInt()),
        Color(0x1255ccff),
        Color(0x38761dff),
        Color(0x56007fff),
        Color(0xb45f06ff.toInt()),
        Color(0x85200cff.toInt())
    )

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing
     */
    fun pointerPosition(screenX: Int, screenY: Int): Vector2 {
        val v = Vector3(screenX.toFloat(), screenY.toFloat(), 0f)
        camera.unproject(v)
        return Vector2(v.x, v.y)
    }

    /**
     * Re(init) camera and batch that are drawing all screens to accomodate to the window resize, device rotation etc.
     * In this program I do not use a fixed viewport with some standard screen-fitting options because the application
     * dynamically accomodates its layout to the screen dimensions.
     */
    fun resizeScreen(screenWidth: Float, screenHeight: Float, batch: PolygonSpriteBatch) {
        if (camera.viewportWidth == screenWidth && camera.viewportHeight == screenHeight)
            return
        camera.setToOrtho(false, screenWidth, screenHeight)
        initBatch(batch)
    }

    /**
     * Re(init) camera and batch that are drawing tiles to accomodate to the window resize, device rotation etc.
     * In this program I prefer fine-tuning the viewport boundaries to provide pixel-accurate drawing
     */
    fun resizeTile(tileWidth: Float, tileHeight: Float, tileBatch: PolygonSpriteBatch) {
        if (tileCamera.viewportWidth == tileWidth && tileCamera.viewportHeight == tileHeight)
            return
        initTileBatch(tileBatch, tileWidth, tileHeight)
    }

    /**
     * Initialize the camera, batch and drawer that draw tiles
     */
    fun initTileBatch(tileBatch: PolygonSpriteBatch, tileWidth: Float = 10f, tileHeight: Float = 10f) {
        tileCamera = OrthographicCamera()
        tileCamera.setToOrtho(false, tileWidth, tileHeight)
        tileCamera.update()
        tileBatch.projectionMatrix = tileCamera.combined
        tsd = ShapeDrawer(tileBatch, ctx.a.white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

    /**
     * Initialize the camera, batch and drawer that draw screens
     */
    fun initBatch(batch: PolygonSpriteBatch) {
        camera = OrthographicCamera()
        camera.setToOrtho(false)
        camera.update()
        batch.projectionMatrix = camera.combined
        sd = ShapeDrawer(batch, ctx.a.white) // A single-pixel texture provides the base color.
        // Then actual colors are specified on the drawing methon calls.
    }

}
