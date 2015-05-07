## build static library for x264


```
git clone git://git.videolan.org/x264.git
export NDK_SYSROOT=~/opt/android-ndk-r9d/platforms/android-9/arch-arm
export PATH=$PATH:~/opt/android-ndk-r9d/toolchains/arm-linux-androideabi-4.6/prebuilt/darwin-x86_64/bin/
./configure --cross-prefix=arm-linux-androideabi- --sysroot="$NDK_SYSROOT" --host=arm-linux --enable-pic --enable-static --disable-cli
make STRIP=
```

reference site: http://vinsol.com/blog/2014/07/30/cross-compiling-ffmpeg-with-x264-for-android/ 

