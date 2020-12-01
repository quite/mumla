# Mumla

Mumla is a fork and continuation of [Plumble](https://github.com/acomminos/Plumble),
a robust GPLv3 Mumble client for Android originally written by Andrew Comminos.
It uses the the [Humla](https://gitlab.com/quite/humla) protocol implementation
(forked from Comminos's [Jumble](https://github.com/acomminos/Jumble)).

Mumla should run on Android 4.0 (IceCreamSandwich, API 14) and later.

Mumla is available [on F-Droid](https://f-droid.org/packages/se.lublin.mumla/).

There is a small [landing page](https://mumla-app.gitlab.io/), that also has
information about [Beta releases](https://mumla-app.gitlab.io/beta/).

## Translations

If you want to help out translating Mumla, the project is [on
Weblate](https://hosted.weblate.org/engage/mumla/) -- thanks for gratis hosting
of our libre project!

## Building on GNU/Linux

TODO: humla-spongycastle should be built as a sub-project of Humla's Gradle,
but currently isn't.

    git submodule update --init --recursive

    pushd libraries/humla/libs/humla-spongycastle
    ../../gradlew jar
    popd

    ./gradlew assembleDebug

## License

Mumla's [LICENSE](LICENSE) is GNU GPL v3.
