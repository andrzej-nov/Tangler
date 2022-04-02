package com.andrzejn.tangler.screens

import com.andrzejn.tangler.Context
import com.badlogic.gdx.graphics.g2d.Sprite

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
    fun setCoords(leftX: Float, topY: Float, rightX: Float, bottomY: Float, buttonsBaseY: Float) {
        boardLeftX = leftX
        boardTopY = topY
        boardRightX = rightX
        boardBottomY = bottomY
        centerX = ctx.viewportWidth / 2
        circleY = buttonsBaseY - tileHeight - indent * 1.28f
        circleRadius = tileHeight * 0.7f
        rotateButtonY = circleY - tileHeight / 2
        rotateLeftX = centerX - tileHeight * 2.3f
        rotateRightX = centerX + tileHeight * 1.1f
        rotateButtonSize = tileHeight.toFloat() * 1.2f

        if (boardLeftX > tileWidth * 1.4f) {
            // Horizontal viewport orientation. Draw buttons in vertical groups outside of the board width
            lowerButtonSize = tileWidth.toFloat()
            lowerButtonY = circleY + tileWidth * 0.2f
            leftButtonsX = boardLeftX - 0.4f * tileWidth - tileWidth
            rightButtonsX = boardRightX + 0.4f * tileWidth
            bottomButtonsXOffset = 0f
            bottomButtonsYOffset = tileWidth + indent
        } else if (rotateButtonY > tileHeight + 2 * indent) {
            // Vertical viewport orientation. Draw buttons in horizontal groups below the rotate buttons
            lowerButtonSize = tileHeight.toFloat()
            lowerButtonY = rotateButtonY - indent - tileHeight
            leftButtonsX = boardLeftX
            rightButtonsX = boardRightX - tileHeight
            bottomButtonsXOffset = tileHeight + indent
            bottomButtonsYOffset = 0f
        } else {
            // Viewport close to square. Draw buttons in vertical groups inside of the board width
            lowerButtonSize = (buttonsBaseY - rotateButtonY) / 2.3f
            leftButtonsX = boardLeftX - tileWidth * 0.7f
            rightButtonsX = boardRightX + tileWidth * 0.7f - lowerButtonSize
            lowerButtonY = buttonsBaseY - lowerButtonSize - indent
            bottomButtonsXOffset = 0f
            bottomButtonsYOffset = lowerButtonY - rotateButtonY + indent
        }

        var logoWidth = boardLeftX - 4 * indent
        if (logoWidth < 0f)
            logoWidth = 0f
        var logoHeight = ctx.viewportHeight - boardTopY - 3 * indent
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
        with(sDown) {
            setSize(rotateButtonSize * 0.5f, rotateButtonSize * 0.5f)
            setPosition((5 * rotateLeftX + 4 * centerX) / 9, circleY - circleRadius)
        }
        ctx.score.setCoords(
            tileHeight / 3, buttonsBaseY - indent - 2 * lineWidth,
            sRotateLeft.x + sRotateLeft.width - 3 * tileWidth, sRotateRight.x, tileWidth * 3f
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
        val halfWidth = tileWidth / 2
        val halfHeight = tileHeight / 2
        if (x > centerX - halfWidth && x < centerX + halfWidth && y < circleY + halfHeight && y > circleY - halfHeight)
            return PressedArea.NextTile

        if (x > sDown.x && x < sDown.x + sDown.width && y < sDown.y + sDown.height && y > sDown.y)
            return PressedArea.UndoMove
        if (x > sPlay.x && x < sPlay.x + sPlay.width && y < sPlay.y + sPlay.height && y > sPlay.y)
            return PressedArea.Play
        if (x > sHome.x && x < sHome.x + sHome.width && y < sHome.y + sHome.height && y > sHome.y)
            return PressedArea.Home
        if (x > sHelp.x && x < sHelp.x + sHelp.width && y < sHelp.y + sHelp.height && y > sHelp.y)
            return PressedArea.Help
        if (x > sExit.x && x < sExit.x + sDown.width && y < sExit.y + sExit.height && y > sExit.y)
            return PressedArea.Exit

        if (x > rotateLeftX && x < centerX && y < circleY + halfHeight * 1.4 && y > circleY - halfHeight * 1.4)
            return PressedArea.RotateLeft
        if (x > centerX && x < rotateRightX + tileHeight
            && y < circleY + halfHeight * 1.4 && y > circleY - halfHeight * 1.4
        )
            return PressedArea.RotateRight
        if (y >= circleY + halfHeight)
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
            filledCircle(centerX, circleY, tileHeight * 0.7f, ctx.drw.theme.gameboardBackground)
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