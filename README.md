# Mumla

Mumla is a fork and continuation of [Plumble](https://github.com/acomminos/Plumble),
a robust GPLv3 Mumble client for Android originally written by Andrew Comminos.
It uses the [Humla](https://gitlab.com/quite/humla) protocol implementation
(forked from Comminos' [Jumble](https://github.com/acomminos/Jumble)).

Mumla should run on Android 4.0 (IceCreamSandwich) and later (API Level 14+).

There is a small page for Mumla here: [https://mumla-app.gitlab.io/](https://mumla-app.gitlab.io/)

## Download

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/se.lublin.mumla/)

## Building on GNU/Linux

TODO: humla-spongycastle should be built as a sub-project of Humla's Gradle,
but currently isn't.

    git submodule update --init --recursive

    pushd libraries/humla/libs/humla-spongycastle
    ../../gradlew jar
    popd

    ./gradlew assembleDebug

## License

Mumla's [LICENSE](LICENSE) is GNU GPL v3+.
