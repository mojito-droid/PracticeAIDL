package com.mojito.aidl

import android.app.ActivityManager
import android.content.Context

const val TAG = "AIDL"

fun Context.progressName(): String {
    val pid = android.os.Process.myPid()
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runningApps = am.runningAppProcesses ?: return "";

    for (processInfo in runningApps) {
        if (processInfo.pid == pid) {
            return processInfo.processName;
        }
    }
    return ""
}

fun getProgressId(): Int = android.os.Process.myPid()