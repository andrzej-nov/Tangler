package com.andrzejn.tangler.helper

import aurelienribon.tweenengine.TweenAccessor

/**
 * Used by the Tween Engine to access field properties
 */
class ScoreAccessor : TweenAccessor<Score> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Score?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_ALPHA -> returnValues!![0] = target!!.pointsAlpha
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Score?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_ALPHA -> (target ?: return).pointsAlpha = (newValues ?: return)[0]
        }
    }
}