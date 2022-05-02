package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.Gdx
import ktx.app.KtxScreen

/**
 * The base object of all game screens.
 */
abstract class BaseScreen(
    /**
     * Reference to the app Context object. The base screen does not use it, but the subclass objects do.
     */
    val ctx: Context
) : KtxScreen {

    /**
     * Called by the GDX framework on screen change. When any screen is hidden, make sure to switch off
     * its input processor.
     */
    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null
    }

    /**
     * Called by the GDX framework on screen resize (window resize, device rotation). Triggers all subsequent
     * coordinates recalculations and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        if (width == 0 || height == 0) // Window minimize on desktop works that way
            return
        ctx.drw.setScreenSize(width, height, ctx.batch)
    }

}