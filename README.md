
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

Mumla should run on Android 5.0 (Lollipop, "L", SDK/API 21) and later.

Mumla is available [on F-Droid](https://f-droid.org/packages/se.lublin.mumla/).

There is a small [landing page](https://mumla-app.gitlab.io/).

## FAQs -- Frequently Asked Questions

### Action that my user has permission for does not show up in overflow menu

Question: The Mumble server I use has an ACL that should give my user
(or a group it's in) permission to carry out a specific action (like
"Move"). Why doesn't Mumla show this action in the overflow menu
(three dots) for a channel or user?

Answer: Try to disconnect and then reconnect to the server. The
decision to show a menu item depending on whether the user has the
required permission is done upon connecting, when the UI is set up. It
is *not* updated on the fly if permissions change while connected.

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

Building is verified to work using JDK 17. So you typically want to set and
export the JAVA_HOME environment variable like `export
JAVA_HOME=/usr/lib/jvm/java-17-openjdk`.

The Android SDK need to be specified as usual, for example by setting
`ANDROID_SDK_ROOT`, or writing it to local.properties as `echo
>local.properties sdk.dir=/home/user/Android/Sdk`

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

The NDK is the toolchain used for building the native code (C/C++) of Humla. We
specify the version needed using `ndkVersion` in
`libraries/humla/build.gradle`.

We currently use Android Gradle Plugin (AGP) version 8.x, which should come
bundled with NDK 25.1.8937393 that we currently use. It is typically installed
in a directory in `~/Android/Sdk/ndk/`. Using newer NDK might give build
errors. See also: https://developer.android.com/studio/projects/install-ndk

If Android Studio does not automatically install the mentioned version of the
NDK in the mentioned directory, then you may be able to get it installed by
using the SDK Manager:

- Click SDK Tools tab.
- Check "Show Package Details"
- In the list view, expand "NDK (Side by side)"
- Check 25.1.8937393
- Click OK

## License

Mumla's [LICENSE](LICENSE) is GNU GPL v3.
