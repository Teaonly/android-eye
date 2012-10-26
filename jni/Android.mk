#####################################################################
# the build script for NDK for droidipcam project
#

LOCAL_PATH:= $(call my-dir)

###########################################################
# build android libteaony 
# libteaonly: clone from libjingle, for generat NDK appplicaton
#      threads, socket, signal/slot and messages, etc.
#
include $(CLEAR_VARS)
LOCAL_MODULE := libmp3encoder
LOCAL_CFLAGS := -O2 -Wall -DANDROID -DSTDC_HEADERS -I./libmp3lame/ 

#including source files
include $(LOCAL_PATH)/lib_build.mk

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
