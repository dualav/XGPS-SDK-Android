//
// Created by hjlee on 2018. 4. 16..
//

#ifndef SKYPROANDROID_NTRIPCLIENT_H
#define SKYPROANDROID_NTRIPCLIENT_H

#include <android/log.h>
#define  LOG_TAG    "ntripTool"

#define  LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define  LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define  LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

enum NTRIP_MODE { MOUNTPOINT = 0, DATA = 1, ERROR = 2};

int ntripTest(void (*callback)(int mode, char* buf, int bufLen), char *strHost, char *strPort, char *strUser, char *strPassword, int mode, char *strMountPoint);

#endif //SKYPROANDROID_NTRIPCLIENT_H
