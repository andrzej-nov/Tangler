package com.andrzejn.tangler.helper

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import com.andrzejn.tangler.Context
import com.andrzejn.tangler.logic.Border
import com.andrzejn.tangler.logic.Path
import com.andrzejn.tangler.logic.Segment

/**
 * This obkect controls fade-out animation of the closed loops and floating score numbers
 */
class Fader(
    /**
     * Reference to the parent Context object
     */
    val ctx: Context
) {
    /**
     * The list of one or more color paths that have been closed to loops and now are fading out and disappear
     */
    private var paths = emptyList<Path>()

    /**
     * Is the fade-out animation in progress
     */
    var inFade: Boolean = false

    /**
     * Current alpha value for the float-up score numbers. Controlled by the tweenManager
     */
    var alpha: Float = 1f

    /**
     * Start fading out the paths. Invoke the callback when the fade-out completes.
     */
    fun fadeDown(pathsToClear: List<Path>, callback: () -> Unit) {
        paths = pathsToClear
        if (paths.isEmpty())
            return
        alpha = 1f
        inFade = true
        Timeline.createSequence()
            // Start with the fully opaque colors both for the paths and numbers
            .push(Tween.set(this, TW_ALPHA).target(1f))
            .push(Tween.set(ctx.score, TW_ALPHA).target(1f))
            .beginParallel()
            .push(
                Timeline.createSequence() // Two-phase fading out the paths
                    .push(Tween.to(ctx.score, TW_ALPHA, 0.4f).target(1f))
                    .push(Tween.to(ctx.score, TW_ALPHA, 0.2f).target(0f))
            )
            .push( // Single-phase fading out the numbers
                Tween.to(this, TW_ALPHA, 0.5f).target(0f)
            )
            .end()
            .setCallback { _, _ ->
                inFade = false
                callback()
            }
            .start(ctx.tweenManager)
    }

    /**
     * Checks that the given segment belongs to the fading-out path and must be redrawn.
     */
    fun affected(s: Segment): Boolean = paths.any { it.segment.contains(s) }

    /**
     * Checks that the given cell border is passed by the fading-out path, so the path border marker must be redrawn.
     */
    fun affected(b: Border): Boolean = b.neighbourSegment.any { it != null && affected(it) }
}