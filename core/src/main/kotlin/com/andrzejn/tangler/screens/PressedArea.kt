package com.andrzejn.tangler.screens

/**
 * The game screen areas that provide different actions on click/press.
 * Some of them (especially Board) require subsequent processing to find out what cell has been clicked etc.
 */
enum class PressedArea {
    /**
     * No action area
     */
    None,

    /**
     * "Rotate left" button
     */
    RotateLeft,

    /**
     * "Rotate right" button
     */
    RotateRight,

    /**
     * "Start new game" button
     */
    Play,

    /**
     * "Hint next move" button
     */
    Help,

    /**
     * "Go to Home/Settings screen" button
     */
    Home,

    /**
     * "Exit the game" button
     */
    Exit,

    /**
     * Gameboard clicked. Analyze its subareas to determine exact cell or scroll button.
     */
    Board,

    /**
     * "Next tile" area (used to start dragging)
     */
    NextTile
}