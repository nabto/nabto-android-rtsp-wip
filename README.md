# Instructions

For building a signed .apk, either edit build.gradle to point to your keystore. Or copy existing keystore:

```
cp <java keystore file> ./android-video/keys/nabtovideo
```

Set your password in `android-video/build.gradle`.

Build .apk file:

```
gradle assembleRelease
```
