// IServiceManager.aidl
package com.mojito.aidl;

// Declare any non-default types here with import statements

interface IServiceManager {
    IBinder getSerive(String serviceName);
}