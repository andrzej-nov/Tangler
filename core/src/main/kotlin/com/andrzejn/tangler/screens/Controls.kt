package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.graphics.g2d.Sprite
import kotlin.math.max
import kotlin.math.min

/**
 * Renders board controls and provides reference coordinates and sizes
 */
class Controls(
    /**
     * Reference to the app Context object
     */
    val ctx: Context
) {
    /**
     * Screen tile/cell width
     */
    var tileWidth: Int = 0

    /**
     * Screen tile/cell height
     */
    var tileHeight: Int = 0

    /**
     * X coordinate of the right side of the board
     */
    var boardRightX: Float = 0f

    /**
     * Y coordinate of the top side of the board
     */
    var boardTopY: Float = 0f

    /**
     * X coordinate of the left side of the board
     */
    var boardLeftX: Float = 0f

    /**
     * Y coordinate of the bottom side of the board
     */
    var boardBottomY: Float = 0f

    /**
     * X coordinate of the screen senter
     */
    var centerX: Float = 0f

    /**
     * Y coordinate of the center of the circle at the bottom (where next tile is displayed)
     */
    var circleY: Float = 0f

    private var circleRadius: Float = 0f
    private var rotateButtonY = 0f
    private var rotateLeftX = 0f
    private var rotateRightX = 0f
    private var rotateButtonSize = 0f

    private var lowerButtonY = 0f
    private var leftButtonsX = 0f
    private var rightButtonsX = 0f
    private var lowerButtonSize = 0f
    private var bottomButtonsXOffset = 0f
    private var bottomButtonsYOffset = 0f

    private val sRotateLeft: Sprite = Sprite(ctx.a.rotateleft)
    private val sRotateRight: Sprite = Sprite(ctx.a.rotateright)
    private val sPlay: Sprite = Sprite(ctx.a.play)
    private val sHelp: Sprite = Sprite(ctx.a.help)
    private val sHome: Sprite = Sprite(ctx.a.home)
    private val sExit: Sprite = Sprite(ctx.a.poweroff)
    private val sLogo: Sprite = Sprite(ctx.a.logo)
    private val sDown: Sprite = Sprite(ctx.a.movedown)

    /**
     * Calculate and set all control coordinates, based on the provided board rectangle coordinates
     */
    fun setCoords(leftX: Float, topY: Float, rightX: Float, bottomY: Float, reservedForControls: Float) {
        boardLeftX = leftX
        boardTopY = topY
        boardRightX = rightX
        boardBottomY = bottomY
        centerX = ctx.viewportWidth / 2
        circleRadius = reservedForControls * 0.45f
        circleY = boardBottomY - reservedForControls * 0.6f
        rotateButtonSize = circleRadius
        rotateButtonY = circleY - rotateButtonSize * 0.5f
        rotateLeftX = centerX - circleRadius - rotateButtonSize * 1.1f
        rotateRightX = centerX + circleRadius + rotateButtonSize * 0.1f

        lowerButtonSize = reservedForControls * 0.4f
        lowerButtonY = boardBottomY - reservedForControls * 0.5f
        leftButtonsX = max(0f, boardLeftX - lowerButtonSize * 2)
        rightButtonsX = min(boardRightX + lowerButtonSize, ctx.viewportWidth - lowerButtonSize)
        bottomButtonsYOffset = lowerButtonSize * 1.1f

        with(sDown) {
            setSize(lowerButtonSize, lowerButtonSize)
            setPosition(
                leftButtonsX + lowerButtonSize * 1.1f,
                max(lowerButtonSize * 0.1f, boardBottomY - lowerButtonSize * 3f)
            )
        }
        var logoWidth = boardLeftX - 2 * indent
        if (logoWidth < 0f)
            logoWidth = 0f
        var logoHeight = ctx.viewportHeight - boardTopY - 2 * indent
        if (logoHeight < 0f)
            logoHeight = 0f
        if (logoWidth > logoHeight)
            ctx.fitToRect(sLogo, logoWidth, ctx.viewportHeight)
        else
            ctx.fitToRect(sLogo, ctx.viewportWidth, logoHeight)
        sLogo.setPosition(0f, ctx.viewportHeight - sLogo.height)

        with(sRotateLeft) {
            setSize(rotateButtonSize, rotateButtonSize)
            setPosition(rotateLeftX, rotateButtonY)
        }
        with(sRotateRight) {
            setSize(rotateButtonSize, rotateButtonSize)
            setPosition(rotateRightX, rotateButtonY)
        }
        ctx.score.setCoords(
            (reservedForControls * 0.2f).toInt(), boardBottomY - reservedForControls * 0.2f,
            sRotateLeft.x + sRotateLeft.width - circleRadius, sRotateRight.x, circleRadius
        )
        with(sPlay) {
            setSize(lowerButtonSize, lowerButtonSize)
            setPosition(leftButtonsX, lowerButtonY)
        }
        with(sHome) {
            setSize(lowerButtonSize, lowerButtonSize)
            setPosition(rightButtonsX, lowerButtonY)
        }
        with(sHelp) {
            setSize(lowerButtonSize, lowerButtonSize)
            setPosition(leftButtonsX + bottomButtonsXOffset, lowerButtonY - bottomButtonsYOffset)
        }
        with(sExit) {
            setSize(lowerButtonSize, lowerButtonSize)
            setPosition(rightButtonsX - bottomButtonsXOffset, lowerButtonY - bottomButtonsYOffset)
        }
    }

    /**
     * Hit test. Determines which of the screen areas has been pressed/clicked
     */
    fun pressedArea(x: Float, y: Float): PressedArea {
        if (x in centerX - circleRadius..centerX + circleRadius && y in circleY - circleRadius..circleY + circleRadius)
            return PressedArea.NextTile

        if (x in sDown.x..sDown.x + sDown.width && y in sDown.y..sDown.y + sDown.height)
            return PressedArea.UndoMove
        if (x in sPlay.x..sPlay.x + sPlay.width && y in sPlay.y..sPlay.y + sPlay.height)
            return PressedArea.Play
        if (x in sHome.x..sHome.x + sHome.width && y in sHome.y..sHome.y + sHome.height)
            return PressedArea.Home
        if (x in sHelp.x..sHelp.x + sHelp.width && y in sHelp.y..sHelp.y + sHelp.height)
            return PressedArea.Help
        if (x in sExit.x..sExit.x + sDown.width && y in sExit.y..sExit.y + sExit.height)
            return PressedArea.Exit

        if (x in sRotateLeft.x..centerX && y in sRotateLeft.y..sRotateLeft.y + sRotateLeft.height)
            return PressedArea.RotateLeft
        if (x in centerX..sRotateRight.x + sRotateRight.width && y in sRotateRight.y..sRotateRight.y + sRotateRight.height)
            return PressedArea.RotateRight
        if (y >= circleY + circleRadius)
            return PressedArea.Board
        return PressedArea.None
    }

    /**
     * Render controls area. Render is called very often, so do not create any object here and precalculate everything.
     */
    fun render(noMoreMoves: Boolean, noLastMove: Boolean) {
        if (sLogo.width >= 2 * tileWidth || sLogo.height >= tileHeight)
            sLogo.draw(ctx.batch, 0.5f)

        sRotateLeft.draw(ctx.batch, 0.8f)
        sRotateRight.draw(ctx.batch, 0.8f)

        with(ctx.drw.sd) {
            filledCircle(centerX, circleY, circleRadius, ctx.drw.theme.gameboardBackground)
            setColor(if (noMoreMoves) ctx.drw.theme.nextTileCircleNoMoves else ctx.drw.theme.nextTileCircleOK)
            circle(
                centerX,
                circleY,
                circleRadius,
                if (noMoreMoves) lineWidth * 2 else lineWidth
            )
            if (noMoreMoves) {
                setColor(ctx.drw.theme.nextGamePrompt)
                filledCircle(
                    sPlay.x + sPlay.width / 2,
                    sPlay.y + sPlay.height / 2,
                    sPlay.height * 0.6f
                )
            }
        }

        sPlay.draw(ctx.batch, 0.8f)
        sHelp.draw(ctx.batch, 0.8f)
        sHome.draw(ctx.batch, 0.8f)
        sExit.draw(ctx.batch, 0.8f)
        sDown.draw(ctx.batch, if (noLastMove) 0.4f else 0.8f)
    }
}