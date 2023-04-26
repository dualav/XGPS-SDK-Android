LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := NtripTool
LOCAL_SRC_FILES := ntripclient.c serial.c NtripTool.c
LOCAL_LDLIBS := -llog -landroid #-lEGL -lGLESv2
LOCAL_CFLAGS :=  -Wall -W -O3
# LOCAL_C_INCLUDES := ./json/

include $(BUILD_SHARED_LIBRARY)
