// MessageReceiverListener.aidl
package com.mojito.aidl;
import com.mojito.aidl.entity.Message;

interface MessageReceiverListener {
    void onReceiveMessage(in Message message);
}