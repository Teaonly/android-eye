#include "beginvision.h"

#define  JNIDEFINE(fname) Java_teaonly_droideye_MainActivity_##fname

extern "C" {
    JNIEXPORT jstring JNICALL JNIDEFINE(nativeQueryInternet)(JNIEnv* env, jclass clz);
};



