package com.andrzejn.tangler.helper

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.setMaxTextureSize
import ktx.assets.Asset
import ktx.assets.loadOnDemand

/**
 * The object that loads and provides sprite textures, fonts etc.
 */
class Atlas {
    private lateinit var atlas: Asset<TextureAtlas>

    /**
     * (Re)load the texture resources definition. In this application we have all textures in the single small PNG
     * picture, so there is just one asset loaded, and loaded synchronously (it is simpler, and does not slow down
     * app startup noticeably)
     */
    fun reloadAtlas() {
        atlas = AssetManager().loadOnDemand("Main.atlas")
    }

    /**
     * Returns reference to particular texture region (sprite image) from the PNG image
     */
    private fun texture(regionName: String): TextureRegion = atlas.asset.findRegion(regionName)

    /**
     * Create a bitmap font with given size, base color etc. from the provided TrueType font.
     * It is more convenient than keep a lot of fixed font bitmaps for different resolutions.
     */
    fun createFont(height: Int): BitmapFont {
        with(FreeTypeFontGenerator(Gdx.files.internal("ADYS-Bold_V5.ttf"))) {
            setMaxTextureSize(2048) // Required for same devices like Xiaomi, where the default 1024 causes garbled fonts
            val font = generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().also {
                it.size = height
                it.color = Color.WHITE
                it.minFilter = Texture.TextureFilter.Linear
                it.magFilter = Texture.TextureFilter.Linear
                it.characters =
                    "\u0000ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*"
            })
            dispose()
            return font
        } // don't forget to dispose the font later to avoid memory leaks!
    }

    val logo: TextureRegion get() = texture("logo")
    val white: TextureRegion get() = texture("white")

    val home: TextureRegion get() = texture("home")
    val play: TextureRegion get() = texture("play")
    val accept: TextureRegion get() = texture("accept")
    val cancel: TextureRegion get() = texture("cancel")
    val rotateleft: TextureRegion get() = texture("rotateleft")
    val rotateright: TextureRegion get() = texture("rotateright")
    val poweroff: TextureRegion get() = texture("poweroff")
    val help: TextureRegion get() = texture("help")
    val options: TextureRegion get() = texture("options")
    val movedown: TextureRegion get() = texture("movedown")

    val tile4: TextureRegion get() = texture("tile4")
    val tile6: TextureRegion get() = texture("tile6")
    val tile8: TextureRegion get() = texture("tile8")
    val tilerepeat: TextureRegion get() = texture("tilerepeat")
    val tilenorepeat: TextureRegion get() = texture("tilenorepeat")
    val sidearrows: TextureRegion get() = texture("sidearrows")
    val gear: TextureRegion get() = texture("gear")

    val icongmail: TextureRegion get() = texture("icongmail")
    val icontelegram: TextureRegion get() = texture("icontelegram")
    val iconfacebook: TextureRegion get() = texture("iconfacebook")
    val icongithub: TextureRegion get() = texture("icongithub")

    val darktheme: TextureRegion get() = texture("darktheme")
    val lighttheme: TextureRegion get() = texture("lighttheme")
}