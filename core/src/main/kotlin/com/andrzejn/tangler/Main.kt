package com.andrzejn.tangler

import com.andrzejn.tangler.screens.CreditsScreen
import com.andrzejn.tangler.screens.GameboardScreen
import com.andrzejn.tangler.screens.HomeScreen
import com.badlogic.gdx.Gdx.graphics
import ktx.app.KtxGame
import ktx.app.KtxScreen

/**
 * [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.
 * That is the application entry point.
 */
class Main : KtxGame<KtxScreen>() {
    private val ctx = Context(this) // Holds all application-wide objects.
    // Singleton objects cause a lot of issues on Android because of its memory allocation/release strategy,
    // so everything should be passed in the Context object on each app object creation or method call
    // where it is needed.

    /**
     * That is the first method called on application start.
     * On mobile devices, it is called each time when the application is restored/relaunched from background.
     */
    override fun create() {
        graphics.isContinuousRendering = false // This game does not require continuous screen rendering as it is
        // mostly static. So let's switch to rendering on demand, to save CPU resources.
        ctx.gs.reset()
        ctx.drw.setTheme()
        ctx.a.reloadAtlas()
        ctx.initBatches() // OpegGL batch objects are heavy. Usually you just need to create one or few of them
        // on the app start and retain them until the end
        addScreen(HomeScreen(ctx))
        addScreen(GameboardScreen(ctx))
        addScreen(CreditsScreen(ctx))
        resumeSavedGameorStartNew()
        graphics.requestRendering() // Request first screen redraw.
    }

    /**
     * Invoked by the system both on Android and desktop, right before the window is closed or the user switches
     * to another application. It is a good place to save the game to resume on next startup.
     */
    override fun pause() {
        if (currentScreen is GameboardScreen)
            ctx.sav.saveGame((currentScreen as GameboardScreen).gameboard)
        else
            ctx.sav.clearSavedGame()
        super.pause()
    }

    /**
     * Invoked by the system on Android when application goes back from background. The create() method might be
     * invoked before resume(), but not always, if the application was not unloaded from memory yet.
     */
    override fun resume() {
        super.resume()
        ctx.a.reloadAtlas() // Texture resources have been lost on pause, need to reload
        ctx.initBatches() // OpenGL drawing batch objects have not been retained either
        resumeSavedGameorStartNew()
    }

    /**
     * Resume game if there is a valid save. Otherwise start from the Home/Settings screen
     */
    private fun resumeSavedGameorStartNew() {
        val s = ctx.sav.savedGame()
        if (ctx.sav.loadSettingsAndScore(s) && getScreen<GameboardScreen>().loadSavedGame(s))
            setScreen<GameboardScreen>()
        else
            setScreen<HomeScreen>()
    }

    /**
     * To avoid "object not disposed" errors on the application exit
     */
    override fun dispose() {
        ctx.dispose()
        super.dispose()
    }
}