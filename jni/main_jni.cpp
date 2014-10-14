#ifdef ANDROID
#include <jni.h>
#include "helper.h"
#include "h264encoder.h"
extern "C" {
#include "g72x/g72x.h"
};

#undef JNIEXPORT
#define JNIEXPORT __attribute__((visibility("default")))
#define JOW(rettype, name) extern "C" rettype JNIEXPORT JNICALL \
          Java_teaonly_droideye_MainActivity_##name

H264Encoder* myAVC = NULL;
g726_state   g726State;

JOW(void, nativeInitMediaEncoder)(JNIEnv* env, jclass, jint width, jint height) {
    myAVC = new H264Encoder(width, height);
    g726_init_state(&g726State);

    return;
};

JOW(void, nativeReleaseMediaEncoder)(JNIEnv* env, jclass) {
    delete myAVC;

    return;
};

JOW(int, nativeDoVideoEncode)(JNIEnv* env, jclass, jbyteArray _yuvData, jbyteArray _nalsData, jint flag) {
    jboolean isCopy = JNI_TRUE;
    jbyte* yuvData = env->GetByteArrayElements(_yuvData, NULL);
    jbyte* nalsData = env->GetByteArrayElements(_nalsData, &isCopy);

    int ret = myAVC->doEncode((unsigned char*)yuvData, (unsigned char*)nalsData, flag);

    env->ReleaseByteArrayElements(_nalsData, nalsData, 0);
    env->ReleaseByteArrayElements(_yuvData, yuvData, JNI_ABORT);

    return ret;
}

JOW(int, nativeDoAudioEncode)(JNIEnv* env, jclass, jbyteArray pcmData, jint length, jbyteArray g726Data) {
    jboolean isCopy = JNI_TRUE;
    jbyte* audioFrame = env->GetByteArrayElements(pcmData, NULL);
    jbyte* audioPacket = env->GetByteArrayElements(g726Data, &isCopy);

    int j = 0;
    unsigned char byteCode = 0x00;
    short* pcmBuffer = (short*) audioFrame;
    for(int i = 0; i < length; i++) {
        short pcm = pcmBuffer[i];
        unsigned char code = g726_32_encoder(pcm, AUDIO_ENCODING_LINEAR, &g726State);

        if (i & 0x01) {
            byteCode = (code << 4) | (byteCode & 0x0f);
            audioPacket[j] = byteCode;
            j++;
        } else {
            byteCode = code;
        }
    }

    env->ReleaseByteArrayElements(g726Data, audioPacket, 0);
    env->ReleaseByteArrayElements(pcmData, audioFrame, JNI_ABORT);

    return j;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv* jni;
    if (jvm->GetEnv(reinterpret_cast<void**>(&jni), JNI_VERSION_1_6) != JNI_OK)
        return -1;
    return JNI_VERSION_1_6;
}

extern "C" jint JNIEXPORT JNICALL JNI_OnUnLoad(JavaVM *jvm, void *reserved) {
    return 0;
}

#endif
