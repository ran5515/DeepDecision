## How to Build:
[Tensorflow Android](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/examples/android)


## Building the Demo with TensorFlow from Source

Pick your preferred approach below. At the moment, we have full support for
Bazel, and partial support for gradle, cmake, make, and Android Studio.

As a first step for all build types, clone the TensorFlow repo with:

```
git clone --recurse-submodules https://github.com/ran5515/DeepDecision.git
```

Note that `--recurse-submodules` is necessary to prevent some issues with
protobuf compilation.

### Bazel

NOTE: Bazel does not currently support building for Android on Windows. Full
support for gradle/cmake builds is coming soon, but in the meantime we suggest
that Windows users download the
[prebuilt binaries](https://ci.tensorflow.org/view/Nightly/job/nightly-android/)
instead.

##### Install Bazel and Android Prerequisites

Bazel is the primary build system for TensorFlow. To build with Bazel,
it and the Android NDK and SDK must be installed on your system.

1. Install the latest version of Bazel as per the instructions [on the Bazel website](https://bazel.build/versions/master/docs/install.html).
2. The Android NDK is required to build the native (C/C++) TensorFlow code.
        The current recommended version is 12b, which may be found
        [here](https://developer.android.com/ndk/downloads/older_releases.html#ndk-12b-downloads).
3. The Android SDK and build tools may be obtained
        [here](https://developer.android.com/tools/revisions/build-tools.html),
        or alternatively as part of
        [Android Studio](https://developer.android.com/studio/index.html). Build
        tools API >= 23 is required to build the TF Android demo (though it will
        run on API >= 21 devices).

##### Edit WORKSPACE

The Android entries in [`<workspace_root>/WORKSPACE`](../../../WORKSPACE#L19-L32)
must be uncommented with the paths filled in appropriately depending on where
you installed the NDK and SDK. Otherwise an error such as:
"The external label '//external:android/sdk' is not bound to anything" will
be reported.

Also edit the API levels for the SDK in WORKSPACE to the highest level you
have installed in your SDK. This must be >= 23 (this is completely independent
of the API level of the demo, which is defined in AndroidManifest.xml).
The NDK API level may remain at 14.

##### Install Model Files (optional)

The TensorFlow `GraphDef`s that contain the model definitions and weights
are not packaged in the repo because of their size. They are downloaded
automatically and packaged with the APK by Bazel via a new_http_archive defined
in `WORKSPACE` during the build process, and by Gradle via download-models.gradle.

**Optional**: If you wish to place the models in your assets manually,
remove all of the `model_files` entries from the `assets`
list in `tensorflow_demo` found in the `[BUILD](BUILD)` file. Then download
and extract the archives yourself to the `assets` directory in the source tree:

```bash
BASE_URL=https://storage.googleapis.com/download.tensorflow.org/models
for MODEL_ZIP in inception5h.zip mobile_multibox_v1a.zip stylize_v1.zip
do
  curl -L ${BASE_URL}/${MODEL_ZIP} -o /tmp/${MODEL_ZIP}
  unzip /tmp/${MODEL_ZIP} -d tensorflow/examples/android/assets/
done
```

This will extract the models and their associated metadata files to the local
assets/ directory.

If you are using Gradle, make sure to remove download-models.gradle reference
from build.gradle after your manually download models; otherwise gradle
might download models again and overwrite your models.

##### Build

After editing your WORKSPACE file to update the SDK/NDK configuration,
you may build the APK. Run this from your workspace root:

```bash
bazel build -c opt //tensorflow/examples/android:tensorflow_demo
```

If you get build errors about protocol buffers, run
`git submodule update --init` and make sure that you've modified your WORKSPACE
file as instructed, then try building again.

##### Install

Make sure that adb debugging is enabled on your Android 5.0 (API 21) or
later device, then after building use the following command from your workspace
root to install the APK:

```bash
adb install -r bazel-bin/tensorflow/examples/android/tensorflow_demo.apk
```
