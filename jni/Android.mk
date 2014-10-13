LOCAL_PATH:= $(call my-dir)
MY_LOCAL_PATH = $(LOCAL_PATH)

###########################################################
# building x264 library 
#
include $(CLEAR_VARS)
LOCAL_MODULE    := libx264_pre
LOCAL_SRC_FILES := x264/libx264.a
include $(PREBUILT_STATIC_LIBRARY)

###########################################################
# building application library 
#
include $(CLEAR_VARS)
LOCAL_MODULE := libMediaEncoder
LOCAL_CPP_EXTENSION := .cc .cpp
LOCAL_CPPFLAGS := -O2 -Werror -Wall 
LOCAL_C_INCLUDES :=  $(MY_LOCAL_PATH)
LOCAL_SRC_FILES := main_jni.cpp \
                   h264encoder.cpp \
                   g72x/g726_32.c \
                   g72x/g711.c \
                   g72x/g72x.c 
                    
LOCAL_LDLIBS += -llog -lz
LOCAL_SHARED_LIBRARIES := libcutils\
                          libgnustl\
                          libdl

LOCAL_STATIC_LIBRARIES := libx264_pre

include $(BUILD_SHARED_LIBRARY)

