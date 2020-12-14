package com.mojito.aidl.entity

import android.os.Parcel
import android.os.Parcelable

data class Message constructor(var content: String = "", var isSendSuccess: Boolean = false) :
    Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(content)
        parcel.writeByte(if (isSendSuccess) 1 else 0)
    }

    /**
     * 为了支持 inout 关键字，需要专门提供方法
     * 找不到符号 message.readFromParcel(_reply);
     *                   ^
     * 符号:   方法 readFromParcel(Parcel)
     * 位置: 类型为Message的变量 message
     */
    fun readFromParcel(parcel: Parcel) {
        content = parcel.readString() ?: ""
        isSendSuccess = parcel.readByte() != 0.toByte()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}
