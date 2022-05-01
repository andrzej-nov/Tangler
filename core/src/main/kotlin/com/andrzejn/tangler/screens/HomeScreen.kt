package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.MathUtils.ceil
import com.badlogic.gdx.utils.Align
import ktx.app.KtxScreen
import kotlin.math.min

/**
 * The Home/Settings screen.
 * First screen of the application. Displayed by the Main class after the application is created.
 * (unless there is a saved game, then we go directly to the game screen with the resumed game).
 */
class HomeScreen(ctx: Context) : BaseScreen(ctx), KtxScreen {
    private var anySettingChanged: Boolean = false
    private val ia = IAdapter()
    private var fontSettings: BitmapFont = BitmapFont()
    private lateinit var fcNum: BitmapFontCache
    private var fontItems: BitmapFont = BitmapFont()
    private lateinit var fcItems: BitmapFontCache

    /**
     * Called by the GDX framework on screen change to this screen. When Home screen is shown,
     * we clear the saved game (going Home means the current game is abandoned)
     * and switch on the screen input processor.
     */
    override fun show() {
        super<BaseScreen>.show()
        anySettingChanged = false
        Gdx.input.inputProcessor = ia
        Gdx.input.setCatchKey(Input.Keys.BACK, true) // Override the Android 'Back' button
    }

    override fun hide() {
        super<BaseScreen>.hide()
        Gdx.input.setCatchKey(Input.Keys.BACK, false) // Override the Android 'Back' button
    }

    private val logo = Sprite(ctx.a.logo)
    private val tile4 = Sprite(ctx.a.tile4)
    private val tile6 = Sprite(ctx.a.tile6)
    private val tile8 = Sprite(ctx.a.tile8)
    private val tilerepeat = Sprite(ctx.a.tilerepeat)
    private val tilenorepeat = Sprite(ctx.a.tilenorepeat)
    private val sidearrows = Sprite(ctx.a.sidearrows)
    private val playgreen = Sprite(ctx.a.playgreen)
    private val resume = Sprite(ctx.a.resume)
    private val exit = Sprite(ctx.a.exit)
    private val credits = Sprite(ctx.a.credits)
    private val gear = Sprite(ctx.a.gear)
    private val darktheme = Sprite(ctx.a.darktheme)
    private val lighttheme = Sprite(ctx.a.lighttheme)
    private val hintson = Sprite(ctx.a.hintson)
    private val hintsoff = Sprite(ctx.a.hintsoff)

    private var gridX = 0f
    private var gridY = 0f
    private val lineWidth = 2f
    private var radius = 0f
    private var baseX = 0f

    /**
     * Called by the GDX framework on screen resize (window resize, device rotation). Triggers all subsequent
     * coordinates recalculations and layout changes.
     */
    override fun resize(width: Int, height: Int) {
        super<BaseScreen>.resize(width, height)
        if (width == 0 || height == 0) // Window minimize on desktop works that way
            return
        val baseHeight = ctx.viewportHeight
        val baseWidth = min(ctx.viewportWidth, baseHeight * 3 / 4)
        baseX = (ctx.viewportWidth - baseWidth) / 2
        gridX = baseWidth / 12
        gridY = baseHeight / 10
        radius = min(2 * gridX, gridY) * 0.4f

        ctx.fitToRect(logo, baseWidth, 2 * gridY * 0.8f)
        logo.setPosition(
            (baseWidth - logo.width) / 2 + baseX,
            gridY * 9 - logo.height / 2
        )
        ctx.fitToRect(tile4, 2 * gridX * 0.8f, gridY * 0.8f)
        tile4.setPosition(
            4 * gridX - tile4.width / 2 + baseX,
            gridY * 7 + (gridY - tile4.height) / 2
        )
        ctx.fitToRect(tile6, 2 * gridX * 0.8f, gridY * 0.8f)
        tile6.setPosition(
            6 * gridX - tile6.width / 2 + baseX,
            gridY * 7 + (gridY - tile6.height) / 2
        )
        ctx.fitToRect(tile8, 2 * gridX * 0.8f, gridY * 0.8f)
        tile8.setPosition(
            8 * gridX - tile8.width / 2 + baseX,
            gridY * 7 + (gridY - tile8.height) / 2
        )
        ctx.fitToRect(tilenorepeat, 3 * gridX * 0.7f, gridY * 0.7f)
        tilenorepeat.setPosition(
            4 * gridX - tilenorepeat.width / 2 + baseX,
            gridY * 5 + (gridY - tilenorepeat.height) / 2
        )
        ctx.fitToRect(tilerepeat, 3 * gridX * 0.7f, gridY * 0.7f)
        tilerepeat.setPosition(
            8 * gridX - tilerepeat.width / 2 + baseX,
            gridY * 5 + (gridY - tilerepeat.height) / 2
        )
        ctx.fitToRect(sidearrows, 2 * gridX * 0.6f, gridY * 0.6f)
        sidearrows.setPosition(
            1.5f * gridX - sidearrows.width / 2 + baseX,
            gridY * 4 + (gridY - sidearrows.height) / 2
        )
        ctx.fitToRect(hintson, 3 * gridX * 0.7f, gridY * 0.7f)
        hintson.setPosition(
            4 * gridX - hintson.width / 2 + baseX,
            gridY * 3 + (gridY - hintson.height) / 2
        )
        ctx.fitToRect(hintsoff, 3 * gridX * 0.7f, gridY * 0.7f)
        hintsoff.setPosition(
            8 * gridX - hintsoff.width / 2 + baseX,
            gridY * 3 + (gridY - hintsoff.height) / 2
        )
        ctx.fitToRect(darktheme, 3 * gridX * 0.7f, gridY * 0.7f)
        darktheme.setPosition(
            4 * gridX - darktheme.width / 2 + baseX,
            gridY * 2 + (gridY - darktheme.height) / 2
        )
        ctx.fitToRect(lighttheme, 3 * gridX * 0.7f, gridY * 0.7f)
        lighttheme.setPosition(
            8 * gridX - lighttheme.width / 2 + baseX,
            gridY * 2 + (gridY - lighttheme.height) / 2
        )

        fontSettings.dispose()
        fontSettings = ctx.a.createFont((min(3 * gridX, gridY) * 0.8f).toInt())
        fcNum = BitmapFontCache(fontSettings)
        fcNum.setText("6", gridX * 3.5f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.addText("8", gridX * 6.5f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.addText("10", gridX * 10f + baseX, gridY * 4.8f, gridX, Align.bottom, false)
        fcNum.setColors(ctx.drw.theme.scoreMoves)

        fontItems.dispose()
        fontItems = ctx.a.createFont((gridY * 0.3f).toInt())
        fcItems = BitmapFontCache(fontItems)
        fcItems.setText("1.", baseX * 0.2f, gridY * 7 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("2.", baseX * 0.2f, gridY * 6 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("3.", baseX * 0.2f, gridY * 5 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("4.", baseX * 0.2f, gridY * 4 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("5.", baseX * 0.2f, gridY * 3 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("6.", baseX * 0.2f, gridY * 2 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.setColors(ctx.drw.theme.settingItem)

        ctx.fitToRect(gear, 2 * gridX * 0.5f, gridY * 0.5f)
        ctx.fitToRect(playgreen, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        playgreen.setPosition(
            6 * gridX - playgreen.width / 2 + baseX,
            (2 * gridY - playgreen.height) / 2
        )
        ctx.fitToRect(resume, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        resume.setPosition(
            6 * gridX - resume.width / 2 + baseX,
            (2 * gridY - resume.height) / 2
        )
        ctx.fitToRect(exit, 2 * gridX * 0.8f, gridY * 0.8f)
        exit.setPosition(
            11 * gridX - exit.width / 2 + baseX,
            (gridY - exit.height) / 2
        )
        ctx.fitToRect(credits, 2 * gridX * 0.8f, gridY * 0.8f)
        credits.setPosition(
            gridX - credits.width / 2 + baseX,
            (gridY - credits.height) / 2
        )
    }

    /**
     * Called by the system each time when the screen needs to be redrawn. It is invoked very frequently,
     * especially when animations are running, so do not create any objects here and precalculate everything
     * as much as possible.
     */
    override fun render(delta: Float) {
        super<BaseScreen>.render(delta)
        ctx.batch.begin()
        ctx.drw.sd.filledRectangle(0f, 0f, ctx.viewportWidth, ctx.viewportHeight, ctx.drw.theme.screenBackground)
        logo.draw(ctx.batch)
        renderGameSettings()
        tile4.draw(ctx.batch)
        tile6.draw(ctx.batch)
        tile8.draw(ctx.batch)
        (1..6).forEach {
            ctx.drw.sd.filledCircle(gridX * (it * 2 - 1) + baseX, 6.5f * gridY, radius, ctx.drw.dark[it])
            ctx.drw.sd.filledCircle(gridX * (it * 2 - 1) + baseX, 6.5f * gridY, radius * 0.8f, ctx.drw.light[it])
        }
        tilerepeat.draw(ctx.batch)
        tilenorepeat.draw(ctx.batch)
        sidearrows.draw(ctx.batch)
        hintson.draw(ctx.batch)
        hintsoff.draw(ctx.batch)
        darktheme.draw(ctx.batch)
        lighttheme.draw(ctx.batch)
        fcNum.draw(ctx.batch)
        if (baseX / fontItems.lineHeight > 15f / 22f)
            fcItems.draw(ctx.batch)
        ctx.drw.sd.line(
            3 * gridX + baseX,
            6 * gridY,
            9 * gridX + baseX,
            6 * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth / 2
        )
        ctx.drw.sd.line(
            3 * gridX + baseX,
            5 * gridY,
            9 * gridX + baseX,
            5 * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth / 2
        )
        ctx.drw.sd.line(
            3 * gridX + baseX,
            4 * gridY,
            9 * gridX + baseX,
            4 * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth / 2
        )
        ctx.drw.sd.rectangle(
            baseX,
            1.9f * gridY,
            12 * gridX,
            6.1f * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth
        )
        with(ctx.drw.theme.polygonHighlight) {
            gear.setColor(r, g, b, a)
        }
        gear.setPosition(-gear.width / 2 + baseX, 8 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 8 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(-gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(12 * gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        if (isNewGame())
            playgreen.draw(ctx.batch)
        else
            resume.draw(ctx.batch)
        exit.draw(ctx.batch)
        credits.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * Should we start new game or resume current one
     */
    private fun isNewGame() = anySettingChanged || !ctx.game.getScreen<GameboardScreen>().wasDisplayed

    /**
     * Render current game settings. When clicked/pressed, the settings changes are immediately saved and displayed.
     */
    private fun renderGameSettings() {
        var y = gridY * 7.05f
        var x = gridX * (0.1f + when (ctx.gs.sidesCount) {
            4 -> 3
            6 -> 5
            else -> 7
        })
        ctx.drw.sd.filledRectangle(
            x + baseX,
            y,
            2 * gridX * 0.9f,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )

        y -= gridY
        x = 0.1f * gridX + baseX
        ctx.drw.sd.filledRectangle(
            x,
            y,
            2 * gridX * ctx.gs.colorsCount - 0.2f * gridX,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.allowDuplicateColors) 7f else 3f) - 0.2f) + baseX
        ctx.drw.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )

        y -= gridY
        x = gridX * (when (ctx.gs.boardSize) {
            6 -> 3f
            8 -> 6f
            else -> 8.8f
        } - 0.4f) + baseX
        ctx.drw.sd.filledRectangle(
            x,
            y,
            2.8f * gridX,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.hints) 3f else 7f) - 0.2f) + baseX
        ctx.drw.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )

        y -= gridY
        x = gridX * ((if (ctx.gs.isDarkTheme) 3f else 7f) - 0.2f) + baseX
        ctx.drw.sd.filledRectangle(
            x,
            y,
            2.4f * gridX,
            gridY * 0.9f,
            0f,
            ctx.drw.theme.settingSelection, ctx.drw.theme.settingSelection
        )
    }

    /**
     * The input adapter for this screen
     */
    inner class IAdapter : InputAdapter() {
        /**
         * Process clicks/presses. Change the settings as selected, or switch to another screen
         * (at the end of the method)
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val v = ctx.drw.pointerPosition(Gdx.input.x, Gdx.input.y)
            v.x -= baseX

            if (v.y in 7 * gridY..8 * gridY) {
                when (v.x) {
                    in 3f * gridX..4.5f * gridX -> {
                        anySettingChanged = true
                        ctx.gs.sidesCount = 4
                    }
                    in 5f * gridX..6.5f * gridX -> {
                        anySettingChanged = true
                        ctx.gs.sidesCount = 6
                    }
                    in 7f * gridX..8.5f * gridX -> {
                        anySettingChanged = true
                        ctx.gs.sidesCount = 8
                    }
                }
            } else if (v.y in 6 * gridY..7 * gridY) {
                anySettingChanged = true
                ctx.gs.colorsCount = ceil(v.x / (2f * gridX))
            }
            if (ctx.gs.colorsCount < ctx.gs.sidesCount / 2)
                ctx.gs.colorsCount = ctx.gs.sidesCount / 2

            if (v.y in 5 * gridY..6 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    anySettingChanged = true
                    ctx.gs.allowDuplicateColors = false
                } else if (v.x in 7 * gridX..9 * gridX) {
                    anySettingChanged = true
                    ctx.gs.allowDuplicateColors = true
                }
            } else if (v.y in 4 * gridY..5 * gridY) {
                when (v.x) {
                    in 2.5 * gridX..5.5 * gridX -> {
                        anySettingChanged = true
                        ctx.gs.boardSize = 6
                    }
                    in 5.5 * gridX..8.5 * gridX -> {
                        anySettingChanged = true
                        ctx.gs.boardSize = 8
                    }
                    in 8.5 * gridX..11.5 * gridX -> {
                        anySettingChanged = true
                        ctx.gs.boardSize = 10
                    }
                }
            } else if (v.y in 3 * gridY..4 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.hints = true
                    anySettingChanged = true
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.hints = false
                    anySettingChanged = true
                }
            } else if (v.y in 2 * gridY..3 * gridY) {
                if (v.x in 3 * gridX..5 * gridX) {
                    ctx.gs.isDarkTheme = true
                    ctx.drw.setTheme()
                } else if (v.x in 7 * gridX..9 * gridX) {
                    ctx.gs.isDarkTheme = false
                    ctx.drw.setTheme()
                }
            } else if (v.y < 2 * gridY && v.x in 5 * gridX..7 * gridX) {
                if (isNewGame())
                    ctx.game.getScreen<GameboardScreen>().newGame()
                ctx.game.setScreen<GameboardScreen>()
            } else if (v.y < gridY && v.x > 10 * gridX)
                Gdx.app.exit()
            else if (v.y < gridY && v.x < 2 * gridX)
                ctx.game.setScreen<CreditsScreen>()
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * On Android 'Back' button switch back to the Home/Settings screen instead of default action
         * (pausing the application)
         */
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK)
                if (ctx.game.getScreen<GameboardScreen>().wasDisplayed)
                    ctx.game.setScreen<GameboardScreen>()
                else
                    Gdx.app.exit()
            return super.keyDown(keycode)
        }
    }

}