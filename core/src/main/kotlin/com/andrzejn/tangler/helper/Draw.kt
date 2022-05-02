package com.andrzejn.tangler.helper

import com.andrzejn.tangler.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
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
     * The main screen viewport
     */
    val screen: ScreenViewport = ScreenViewport()

    /**
     * Viewport for the game field
     */
    lateinit var board: ScalingViewport

    /**
     * A convenience shortcut
     */
    val boardCamPos: Vector3 get() = this.board.camera.position

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
     *
     */
    data class Theme(
        val screenBackground: Color,
        val nextTileCircleOK: Color,
        val nextTileCircleNoMoves: Color,
        val settingSelection: Color,
        val settingItem: Color,
        val settingSeparator: Color,
        val gameboardBackground: Color,
        val polygonHighlight: Color,
        val creditsText: Color,
        val nextGamePrompt: Color,
        val scorePoints: Color,
        val scoreMoves: Color
    )

    private val lt: Theme = Theme(
        screenBackground = Color.GRAY,
        nextTileCircleOK = Color.DARK_GRAY,
        nextTileCircleNoMoves = Color.FIREBRICK,
        settingSelection = Color.LIGHT_GRAY,
        settingItem = Color.DARK_GRAY,
        settingSeparator = Color.DARK_GRAY,
        gameboardBackground = Color.LIGHT_GRAY,
        polygonHighlight = Color.BLACK,
        creditsText = Color.NAVY,
        nextGamePrompt = Color.CHARTREUSE,
        scorePoints = Color.CHARTREUSE,
        scoreMoves = Color.GOLD
    )

    private val dk: Theme = Theme(
        screenBackground = Color.DARK_GRAY,
        nextTileCircleOK = Color.LIGHT_GRAY,
        nextTileCircleNoMoves = Color.RED,
        settingSelection = Color.GRAY,
        settingItem = Color.LIGHT_GRAY,
        settingSeparator = Color.LIGHT_GRAY,
        gameboardBackground = Color.BLACK,
        polygonHighlight = Color.WHITE,
        creditsText = Color.WHITE,
        nextGamePrompt = Color.CHARTREUSE,
        scorePoints = Color.CHARTREUSE,
        scoreMoves = Color.GOLD
    )

    /**
     *
     */
    lateinit var theme: Theme

    /**
     * Set color theme according to current game setting value
     */
    fun setTheme() {
        theme = if (ctx.gs.isDarkTheme) dk else lt
    }

    private val v3 = Vector3()
    private val v2s = Vector2()
    private val v2b = Vector2()

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing on screen
     */
    fun pointerPositionScreen(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        screen.unproject(v3)
        return v2s.set(v3.x, v3.y)
    }

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing on board
     */
    fun pointerPositionBoard(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        board.unproject(v3)
        return v2b.set(v3.x, v3.y)
    }

    /**
     * Re(init) camera and batch that are drawing all screens to accomodate to the window resize, device rotation etc.
     * In this program I do not use a fixed viewport with some standard screen-fitting options because the application
     * dynamically accomodates its layout to the screen dimensions.
     */
    fun setScreenSize(screenWidth: Int, screenHeight: Int, batch: PolygonSpriteBatch) {
        if (screen.screenWidth == screenWidth && screen.screenHeight == screenHeight)
            return
        screen.update(screenWidth, screenHeight, true)
        initBatch(batch)
    }

    /**
     * Re(init) camera and batch that are drawing tiles to accomodate to the window resize, device rotation etc.
     * In this program I prefer fine-tuning the viewport boundaries to provide pixel-accurate drawing
     */
    fun setTileSize(tileWidth: Float, tileHeight: Float, tileBatch: PolygonSpriteBatch) {
        if (tileCamera.viewportWidth == tileWidth && tileCamera.viewportHeight == tileHeight)
            return
        initTileBatch(tileBatch, tileWidth, tileHeight)
    }

    /**
     * Deserialized value
     */
    private val savedCamPos = Vector2()

    /**
     * Sets the game field viewport size and position
     */
    fun setBoardSize(leftX: Float, bottomY: Float, boardSquareWidth: Float, boardSquareHeight: Float) {
        val prevWorldWidth = if (this::board.isInitialized) board.worldWidth else -1f
        val basePosChanged =
            if (this::board.isInitialized) boardSquareWidth != prevWorldWidth || board.screenX != leftX.toInt() ||
                    board.screenY != bottomY.toInt()
            else true
        if (basePosChanged) {
            if (savedCamPos == Vector2.Zero && this::board.isInitialized && prevWorldWidth > 0) {
                savedCamPos.set(boardCamPos.x, boardCamPos.y).scl(boardSquareWidth / prevWorldWidth)
            }
            board = ScalingViewport(Scaling.none, boardSquareWidth, boardSquareHeight)
            board.setScreenBounds(
                leftX.toInt(),
                bottomY.toInt(),
                boardSquareWidth.toInt(),
                boardSquareHeight.toInt()
            )
        }
        if (basePosChanged || savedCamPos != Vector2.Zero) {
            if (savedCamPos != Vector2.Zero) {
                boardCamPos.set(savedCamPos, 0f)
                savedCamPos.set(Vector2.Zero)
            } else
                centerFieldCamera()
        }
    }

    /**
     * Center camera on the field viewport
     */
    fun centerFieldCamera() {
        if (this::board.isInitialized) // Check if the lateinit property has been initialised already
            board.camera.position.set(board.worldWidth / 2, board.worldHeight / 2, 0f)
    }

    fun drawToScreen() {
        screen.apply()
        ctx.batch.projectionMatrix = screen.camera.combined
    }

    fun drawToBoard() {
        board.apply()
        ctx.batch.projectionMatrix = board.camera.combined
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
        sd = ShapeDrawer(batch, ctx.a.white) // A single-pixel texture provides the base color.
        batch.projectionMatrix = screen.camera.combined
        // Then actual colors are specified on the drawing methon calls.
    }

}
