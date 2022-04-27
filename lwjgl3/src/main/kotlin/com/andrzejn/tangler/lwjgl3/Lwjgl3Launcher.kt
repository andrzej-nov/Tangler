package com.andrzejn.tangler.lwjgl3

import com.andrzejn.tangler.Main
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/** Launches the desktop (LWJGL3) application.  */
object Lwjgl3Launcher {
    /**
     *
     */
    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(Main(), Lwjgl3ApplicationConfiguration().apply {
            setTitle("Tangler")
            setForegroundFPS(60)
            setWindowedMode(960, 960)
            setWindowIcon("icon256.png", "icon128.png", "icon64.png", "icon32.png", "icon16.png")
        })
    }
}