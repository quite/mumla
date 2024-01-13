
# Software maintenance situation

I, Lublin, have very little time to do voluntary work on Mumla. My focus is
strictly on maintaining stability and security. This includes migrations to
newer Android SDKs, as they become requirements by Google/Alphabet for even
getting updates published on Google Play. There are also other maintenance and
administrative work. I barely have time to get this done in a timely manner.

At some point I expect Mumla to disappear from Google Play, because there will
be some requirement that I did not have time to fulfill. Eventually it will
also rot and no longer work well in general on newer releases of Android.

Mumla needs a new maintainer that can allocate time to take on, to begin with,
all these tasks. To maintain stability and security. And then hopefully also
work with the community on for example protocol parity with desktop Mumble,
support for various hardware accessories, general usability, and new features.

Until there is a new maintainer with time on their hands you cannot expect new
features, or even the continued existance of a usable Mumble app for Android.

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

## Repository submodules

Note that this Mumla git repository has submodule(s). You either need to clone
it using `git clone --recursive`, or you need to get the submodule(s) in place
after cloning:

    git submodule update --init --recursive

## Building on GNU/Linux

TODO: Building is verified to work using JDK 17. So you typically want `export
JAVA_HOME=/usr/lib/jvm/java-17-openjdk`. Tracking issue:
https://gitlab.com/quite/mumla/-/issues/108

TODO: humla-spongycastle should be built as a sub-project of Humla's Gradle,
but currently isn't.

    git submodule update --init --recursive

    pushd libraries/humla/libs/humla-spongycastle
    ../../gradlew jar
    popd

    ./gradlew assembleDebug

If you get an error running out of Java heap space, try raising the -Xmx in
`./gradle.properties`.

### Notes on NDK

We're now building with Android Gradle Plugin (AGP) 8.x, which should come
bundled with NDK version 25.1.8937393 that we currently use. The NDK is the
toolchain used for building the native code (C/C++) of Humla, specified using
`ndkVersion` in `libraries/humla/build.gradle`. Using newer NDK might give
build errors. See also:
https://developer.android.com/studio/projects/install-ndk

If this version of the NDK does not get installed automatically by Android
Studio (typically in a directory in `~/Android/Sdk/`), then you may be able
to get it installed by using its SDK Manager like this:

- Click SDK Tools tab.
- Check "Show Package Details"
- In the list view, expand "NDK (Side by side)"
- Check 25.1.8937393
- Click OK

## License

Mumla's [LICENSE](LICENSE) is GNU GPL v3.
