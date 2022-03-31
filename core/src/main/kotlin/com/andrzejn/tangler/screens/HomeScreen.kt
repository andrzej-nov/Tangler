package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.Gdx
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
        ctx.sav.clearSavedGame()
        Gdx.input.inputProcessor = ia
    }

    private val logo = Sprite(ctx.a.logo)
    private val tile4 = Sprite(ctx.a.tile4)
    private val tile6 = Sprite(ctx.a.tile6)
    private val tile8 = Sprite(ctx.a.tile8)
    private val tilerepeat = Sprite(ctx.a.tilerepeat)
    private val tilenorepeat = Sprite(ctx.a.tilenorepeat)
    private val sidearrows = Sprite(ctx.a.sidearrows)
    private val play = Sprite(ctx.a.play)
    private val poweroff = Sprite(ctx.a.poweroff)
    private val options = Sprite(ctx.a.options)
    private val gear = Sprite(ctx.a.gear)
    private val darktheme = Sprite(ctx.a.darktheme)
    private val lighttheme = Sprite(ctx.a.lighttheme)

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
        val baseHeight = ctx.viewportHeight
        val baseWidth = min(ctx.viewportWidth, baseHeight * 3 / 4)
        baseX = (ctx.viewportWidth - baseWidth) / 2
        gridX = baseWidth / 12
        gridY = baseHeight / 9
        radius = min(2 * gridX, gridY) * 0.4f

        ctx.fitToRect(logo, baseWidth, 2 * gridY * 0.8f)
        logo.setPosition(
            (baseWidth - logo.width) / 2 + baseX,
            gridY * 8 - logo.height / 2
        )
        ctx.fitToRect(tile4, 2 * gridX * 0.8f, gridY * 0.8f)
        tile4.setPosition(
            4 * gridX - tile4.width / 2 + baseX,
            gridY * 6 + (gridY - tile4.height) / 2
        )
        ctx.fitToRect(tile6, 2 * gridX * 0.8f, gridY * 0.8f)
        tile6.setPosition(
            6 * gridX - tile6.width / 2 + baseX,
            gridY * 6 + (gridY - tile6.height) / 2
        )
        ctx.fitToRect(tile8, 2 * gridX * 0.8f, gridY * 0.8f)
        tile8.setPosition(
            8 * gridX - tile8.width / 2 + baseX,
            gridY * 6 + (gridY - tile8.height) / 2
        )
        ctx.fitToRect(tilenorepeat, 3 * gridX * 0.7f, gridY * 0.7f)
        tilenorepeat.setPosition(
            4 * gridX - tilenorepeat.width / 2 + baseX,
            gridY * 4 + (gridY - tilenorepeat.height) / 2
        )
        ctx.fitToRect(tilerepeat, 3 * gridX * 0.7f, gridY * 0.7f)
        tilerepeat.setPosition(
            8 * gridX - tilerepeat.width / 2 + baseX,
            gridY * 4 + (gridY - tilerepeat.height) / 2
        )
        ctx.fitToRect(sidearrows, 2 * gridX * 0.6f, gridY * 0.6f)
        sidearrows.setPosition(
            1.5f * gridX - sidearrows.width / 2 + baseX,
            gridY * 3 + (gridY - sidearrows.height) / 2
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
        fcNum.setText("6", gridX * 3.5f + baseX, gridY * 3.8f, gridX, Align.bottom, false)
        fcNum.addText("8", gridX * 6.5f + baseX, gridY * 3.8f, gridX, Align.bottom, false)
        fcNum.addText("10", gridX * 10f + baseX, gridY * 3.8f, gridX, Align.bottom, false)
        fcNum.setColors(ctx.drw.theme.scoreMoves)

        fontItems.dispose()
        fontItems = ctx.a.createFont((gridY * 0.3f).toInt())
        fcItems = BitmapFontCache(fontItems)
        fcItems.setText("1.", baseX * 0.2f, gridY * 6 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("2.", baseX * 0.2f, gridY * 5 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("3.", baseX * 0.2f, gridY * 4 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("4.", baseX * 0.2f, gridY * 3 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.addText("5.", baseX * 0.2f, gridY * 2 + fontItems.lineHeight * 1.5f, baseX * 0.7f, Align.right, false)
        fcItems.setColors(ctx.drw.theme.settingItem)

        ctx.fitToRect(gear, 2 * gridX * 0.5f, gridY * 0.5f)
        ctx.fitToRect(play, 4 * gridX * 0.8f, 2 * gridY * 0.8f)
        play.setPosition(
            6 * gridX - play.width / 2 + baseX,
            (2 * gridY - play.height) / 2
        )
        ctx.fitToRect(poweroff, 2 * gridX * 0.8f, gridY * 0.8f)
        poweroff.setPosition(
            11 * gridX - poweroff.width / 2 + baseX,
            (gridY - poweroff.height) / 2
        )
        ctx.fitToRect(options, 2 * gridX * 0.8f, gridY * 0.8f)
        options.setPosition(
            gridX - options.width / 2 + baseX,
            (gridY - options.height) / 2
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
            ctx.drw.sd.filledCircle(gridX * (it * 2 - 1) + baseX, 5.5f * gridY, radius, ctx.drw.dark[it])
            ctx.drw.sd.filledCircle(gridX * (it * 2 - 1) + baseX, 5.5f * gridY, radius * 0.8f, ctx.drw.light[it])
        }
        tilerepeat.draw(ctx.batch)
        tilenorepeat.draw(ctx.batch)
        sidearrows.draw(ctx.batch)
        darktheme.draw(ctx.batch)
        lighttheme.draw(ctx.batch)
        fcNum.draw(ctx.batch)
        if (baseX / fontItems.lineHeight > 15f / 22f)
            fcItems.draw(ctx.batch)
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
        ctx.drw.sd.line(
            3 * gridX + baseX,
            3 * gridY,
            9 * gridX + baseX,
            3 * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth / 2
        )
        ctx.drw.sd.line(
            gridX + baseX,
            7 * gridY,
            11 * gridX + baseX,
            7 * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth
        )
        ctx.drw.sd.line(
            gridX + baseX,
            1.9f * gridY,
            11 * gridX + baseX,
            1.9f * gridY,
            ctx.drw.theme.settingSeparator,
            lineWidth
        )
        with(ctx.drw.theme.polygonHighlight) {
            gear.setColor(r, g, b, a)
        }
        gear.setPosition(gridX - gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(11 * gridX - gear.width / 2 + baseX, 7 * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        gear.setPosition(11 * gridX - gear.width / 2 + baseX, 1.9f * gridY - gear.height / 2)
        gear.draw(ctx.batch)
        play.draw(ctx.batch)
        poweroff.draw(ctx.batch)
        options.draw(ctx.batch)
        ctx.batch.end()
    }

    /**
     * Render current game settings. When clicked/pressed, the settings changes are immediately saved and displayed.
     */
    private fun renderGameSettings() {
        var y = gridY * 6.05f
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

            if (6 * gridY < v.y && v.y < 7 * gridY) {
                if (3 * gridX < v.x && v.x < 4.5 * gridX)
                    ctx.gs.sidesCount = 4
                else if (5 * gridX < v.x && v.x < 6.5 * gridX)
                    ctx.gs.sidesCount = 6
                else if (7 * gridX < v.x && v.x < 8.5 * gridX)
                    ctx.gs.sidesCount = 8
            } else if (5 * gridY < v.y && v.y < 6 * gridY)
                ctx.gs.colorsCount = ceil(v.x / (2f * gridX))

            if (ctx.gs.colorsCount < ctx.gs.sidesCount / 2)
                ctx.gs.colorsCount = ctx.gs.sidesCount / 2

            if (4 * gridY < v.y && v.y < 5 * gridY) {
                if (3 * gridX < v.x && v.x < 5 * gridX)
                    ctx.gs.allowDuplicateColors = false
                else if (7 * gridX < v.x && v.x < 9 * gridX)
                    ctx.gs.allowDuplicateColors = true
            } else if (3 * gridY < v.y && v.y < 4 * gridY) {
                if (2.5 * gridX < v.x && v.x < 5.5 * gridX)
                    ctx.gs.boardSize = 6
                else if (5.5 * gridX < v.x && v.x < 8.5 * gridX)
                    ctx.gs.boardSize = 8
                else if (8.5 * gridX < v.x && v.x < 11.5 * gridX)
                    ctx.gs.boardSize = 10
            } else if (2 * gridY < v.y && v.y < 3 * gridY) {
                if (3 * gridX < v.x && v.x < 5 * gridX) {
                    ctx.gs.isDarkTheme = true
                    ctx.drw.setTheme()
                } else if (7 * gridX < v.x && v.x < 9 * gridX) {
                    ctx.gs.isDarkTheme = false
                    ctx.drw.setTheme()
                }
            } else if (v.y < 2 * gridY && 5 * gridX < v.x && v.x < 7 * gridX) {
                ctx.game.getScreen<GameboardScreen>().newGame()
                ctx.game.setScreen<GameboardScreen>()
            } else if (v.y < gridY && v.x > 10 * gridX)
                Gdx.app.exit()
            else if (v.y < gridY && v.x < 2 * gridX)
                ctx.game.setScreen<CreditsScreen>()
            return super.touchDown(screenX, screenY, pointer, button)
        }
    }

}