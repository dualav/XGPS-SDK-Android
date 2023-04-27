//
// Created by hjlee on 2018. 4. 16..
//

#include <jni.h>

#include <android/asset_manager_jni.h>

#include "ntripclient.h"

JNIEnv *_env = NULL;
jmethodID  methodID = NULL;
jobject xgpsObject = NULL;
extern int stop;

void callback(int mode, char *buffer, int buffLen) {
    LOGV("callback %d : %d", mode, buffLen);
    jbyteArray jb;
    jb=(*_env)->NewByteArray(_env, buffLen);
    (*_env)->SetByteArrayRegion(_env, jb, 0, buffLen, buffer);
    (*_env)->CallVoidMethod(_env, xgpsObject, methodID, jb, buffLen, mode);
    (*_env)->DeleteLocalRef(_env, jb);
}

JNIEXPORT void JNICALL Java_com_namsung_xgpsmanager_NtripClient_onCreate(JNIEnv *env, jobject obj, jobject xgpsManagerObj,
                                                                         jstring host, jstring port, jstring user, jstring password, jint mode, jstring mountpoint) {
    char buf[128];

    const char *strHost = (*env)->GetStringUTFChars(env, host, 0);
    const char *strPort = (*env)->GetStringUTFChars(env, port, 0);
    const char *strUser = (*env)->GetStringUTFChars(env, user, 0);
    const char *strPassword = (*env)->GetStringUTFChars(env, password, 0);
    const char *strMountPoint = (*env)->GetStringUTFChars(env, mountpoint, 0);

    LOGI("ntripClientJNI_onCreate %s, %s", strHost, strMountPoint);


    stop = 0;
    jclass xgpsManagerClass = (*env)->GetObjectClass(env, xgpsManagerObj);
    jmethodID getGnssDataID = (*env)->GetMethodID(env, xgpsManagerClass, "getGNSSData", "([BII)V");
    _env = env;
    methodID = getGnssDataID;
    xgpsObject = xgpsManagerObj;
    ntripTest(callback, strHost, strPort, strUser, strPassword, mode, strMountPoint);

    (*env)->ReleaseStringUTFChars(env, host, strHost);
    (*env)->ReleaseStringUTFChars(env, port, strPort);
    (*env)->ReleaseStringUTFChars(env, user, strUser);
    (*env)->ReleaseStringUTFChars(env, password, strPassword);
    (*env)->ReleaseStringUTFChars(env, mountpoint, strMountPoint);
}

JNIEXPORT void JNICALL Java_com_namsung_xgpsmanager_NtripClient_onDestroy(JNIEnv *env, jobject obj) {
    LOGI("ntripClientJNI_onDestroy");
    stop = 1;
}


JNIEXPORT jboolean JNICALL Java_com_namsung_xgpsmanager_NtripClient_isReady(JNIEnv *env, jobject obj) {
    LOGI("ntripClientJNI_onDestroy");
    return !stop;
}