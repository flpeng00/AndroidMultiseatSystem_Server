LOCAL_PATH := $(call my-dir)
APP_PLATFORM := android-8 

include $(CLEAR_VARS)
LOCAL_MODULE := mupdf
LOCAL_SRC_FILES = libs/libmupdf.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := fbcon
LOCAL_SRC_FILES := InputHandler.cpp \
				   Input.cpp \
				   suinput.cpp \
				   fbcon.cpp
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
APP_OPTIM := debug
LOCAL_CFLAGS := -g