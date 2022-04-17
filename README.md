# Tangler

**Tangler** is a small casual puzzle inspired by the [Tantrix board game](https://www.tantrix.com/) but with a different
twist.

https://user-images.githubusercontent.com/89737218/160915390-636eaa88-bae2-42ab-a674-eff22d36c676.mp4

![Clipboard02!](https://user-images.githubusercontent.com/89737218/160927554-3c74f7ee-cf5b-40a5-854a-1f6a8e261fbc.jpg)

**Game objective:** put randomly generated tiles to the board, making up continuous color curves. When curves close to
loops, they disappear, giving space for more moves.

There are 3 board sizes, 3 tile types and up to 6 colors with an option of unique/repeating colors per tile.

The game is intended to be meditative, like creating a mandala, so there is no game timer and no hall of fame. There are
also no sound, no ads and no in-game purchases. The game is completely free and will remain so. It does not use Internet
connection and does not require any device permissions.

The game is playable on smartphones, especially with smaller board sizes, but a larger (tablet) screen is recommended
for best experience.

## Download

The game is provided in two options:

- **Desktop Java**. [Download Tangler.jar](https://github.com/andrzej-nov/Tangler/releases/download/v1.9/Tangler.jar).
  Run it with `java -jar Tangler.jar` command line, or in most cases just double-click the Tangler.jar file. It has been
  tested with Java 18 Runtime, should also work with prior versions up to Java 8.
    - **On MacOS** you will get a warning about unidentified developer. Start the Tangler.jar using Finder context menu
      instead of
      Launchpad, [as explained here](https://www.bemidjistate.edu/offices/its/knowledge-base/how-to-open-an-app-from-an-unidentified-developer-and-exempt-it-from-gatekeeper/)
      .

- **Android**. [Get it on Google Play](https://play.google.com/store/apps/details?id=com.andrzejn.tangler) (recommended)
  or [download the Tangler.apk](https://github.com/andrzej-nov/Tangler/releases/download/v1.9/Tangler.apk) here for
  manual install (it might be sometimes also a newer version due to the Google Play approval lag). It has been tested on Android
  8.0 and 10.0, should also work on any Android version starting from 4.4 and later.
    - **Known issues on Xiaomi smartphones:** Last move might be lost when you switch from Tangler to another app and
      back again. That is the Xiaomi issue, I do not know a workaround yet.

There is no iOS build because I do not have tools to test and deploy it to the AppStore. If somebody completes the iOS
module (see below), I will add it here.

## Donation

If you like the game and want to support the author, you may donate arbitrary amount via following
link: https://pay.fondy.eu/s/3DJ4BV1DmBgU (processed by the [Fondy.eu](https://fondy.io/) payment system).

## Development

The game is provided under the [Creative Commons Attribution license](https://creativecommons.org/licenses/by/4.0/).
Please feel free to reuse, extend, derive, improve etc. as long as you keep a reference to the original and mention me,
Andrzej Novosiolov, as the original author.

The game has been implemented using following tools and libraries:

- [IntelliJ IDEA 2022.1 (Community Edition)](https://www.jetbrains.com/idea/download/)
- [Android Studio 2021.1.1 Patch 2](https://developer.android.com/studio) (for the Android emulator)
- [Gradle 7.0.4](https://gradle.org/)
- [Kotlin 1.6.20](https://kotlinlang.org/)
- [libGDX 1.10.0](https://libgdx.com/)
- [libKTX 1.10.0-rc1](https://libktx.github.io/)
- [ShapeDrawer 2.5.0](https://github.com/earlygrey/shapedrawer#shape-drawer)
- [Universal Tween Engine 6.3.3](https://github.com/AurelienRibon/universal-tween-engine)

The `ios` module is present in the project and compiling, but I did not tested it because I do not have Apple devices
and tools for that. If you make it work, I would gratefully accept the pull request.
