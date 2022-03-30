package com.andrzejn.tangler.helper

import aurelienribon.tweenengine.TweenAccessor
import com.badlogic.gdx.graphics.g2d.Sprite

/**
 * Tween sprite rotation. Rotation angle values are in degrees, positive angles are counterclockwise.
 */
const val TW_ANGLE: Int = 1

/**
 * Tween sprite position
 */
const val TW_POS_XY: Int = 2

/**
 * Used by the Tween Engine to access field properties
 */
class SpriteAccessor : TweenAccessor<Sprite> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Sprite?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_ANGLE -> returnValues!![0] = target!!.rotation
            TW_POS_XY -> {
                returnValues!![0] = target!!.x
                returnValues[1] = target.y
                return 2
            }
            TW_ALPHA -> returnValues!![0] = target!!.color.a
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Sprite?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_ANGLE -> (target ?: return).rotation = (newValues ?: return)[0]
            TW_POS_XY -> {
                (target ?: return).x = (newValues ?: return)[0]
                target.y = newValues[1]
            }
            TW_ALPHA -> (target ?: return).setAlpha((newValues ?: return)[0])
        }
    }
}