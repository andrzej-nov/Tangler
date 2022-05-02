package com.andrzejn.tangler

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.tangler.helper.*
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.Sprite

/**
 * Holds all application-wide objects.
 * Singleton objects cause a lot of issues on Android because of its memory allocation/release strategy,
 * so everything should be passed in the Context object on each app object creation or method call
 * where it is needed.
 */
class Context(
    /**
     * Reference to the Main game object. Needed to switch game screens on different points of execution.
     */
    val game: Main
) {
    /**
     * The batch for drawing all screen contents.
     */
    lateinit var batch: PolygonSpriteBatch

    /**
     * The batch for drawing tile sprites. It is simpler to have two separate batches than to remember when to switch
     * the camera on single batch
     */
    lateinit var tileBatch: PolygonSpriteBatch

    /**
     * Atlas object that loads and provides sprite textures, fonts etc.
     */
    val a: Atlas = Atlas()

    /**
     * Drawing helper objects (cameras, ShapeDrawers etc.)
     */
    val drw: Draw = Draw(this)

    /**
     * Helper object for some fade-out animations
     */
    val fader: Fader = Fader(this)

    /**
     * Helper object for the game saving/loading
     */
    val sav: SaveGame = SaveGame(this)

    /**
     * Helper object to track and display the score
     */
    val score: Score = Score(this)

    /**
     * Game settings access (settings are stored using GDX system Preferences class)
     */
    val gs: GameSettings = GameSettings()

    /**
     * The main object that handles all animations
     */
    val tweenManager: TweenManager = TweenManager()

    init { // Need to specify which objects' properties will be used for animations
        Tween.registerAccessor(Sprite::class.java, SpriteAccessor())
        Tween.registerAccessor(Fader::class.java, FaderAccessor())
        Tween.registerAccessor(Score::class.java, ScoreAccessor())
    }

    /**
     * Not clearly documented but working method to check whether some transition animations are in progress
     * (and ignore user input until animations complete, for example)
     */
    fun tweenAnimationRunning(): Boolean {
        return tweenManager.objects.isNotEmpty()
    }

    /**
     * Many times we'll need to fit a sprite into arbitrary rectangle, retaining proportions
     */
    fun fitToRect(s: Sprite, wBound: Float, hBound: Float) {
        var width = wBound
        var height = wBound * s.regionHeight / s.regionWidth
        if (height > hBound) {
            height = hBound
            width = hBound * s.regionWidth / s.regionHeight
        }
        s.setSize(width, height)
    }

    /**
     * (Re)create OpenGL drawing batches. Called only on application startup or unpause
     */
    fun initBatches() {
        if (this::batch.isInitialized) // Check if the lateinit property has been initialised already
            batch.dispose()
        batch = PolygonSpriteBatch()
        if (this::tileBatch.isInitialized)
            tileBatch.dispose()
        tileBatch = PolygonSpriteBatch()
        drw.initBatch(batch)
        drw.initTileBatch(tileBatch)
    }

    /**
     * Shortened accessor to the screen viewportWidth
     */
    val viewportWidth: Float get() = drw.screen.worldWidth

    /**
     * Shortened accessor to the screen viewportHeight
     */
    val viewportHeight: Float get() = drw.screen.worldHeight

    /**
     * Cleanup
     */
    fun dispose() {
        if (this::batch.isInitialized)
            batch.dispose()
        if (this::tileBatch.isInitialized)
            tileBatch.dispose()
        score.dispose()
    }

}