package com.mojito.aidl

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.mojito.aidl.entity.Message
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * AIDL 跑在 Binder 线程池当中
 */
class RemoteService : Service() {

    private var mIsConnected: Boolean = false
    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val mMessengerHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                super.handleMessage(msg)
                try {
                    val bundle = msg.data
                    bundle.classLoader = Message::class.java.classLoader
                    val message = bundle.getParcelable<Message>("message")
                    Log.d(
                        TAG,
                        "RemoteService handleMessage() called with: msg = $message ${progressInfo()}"
                    )
                    val client = msg.replyTo

                    val reply = Message("msg from remote by messenger")

                    val replyMessage = android.os.Message.obtain()
                    val replyBundle = Bundle();
                    replyBundle.putParcelable("message", reply)
                    replyMessage.data = replyBundle
                    client.send(replyMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "handleMessage: ", e)
                }

            }
        }
    }

    // 一个小坑：MutableList 不适合用在 AIDL 中作为 listener 的存储
    // 应该使用 RemoteCallbackList
//    private val mMessageListener = mutableListOf<MessageReceiverListener>()
    private val mMessageListener = RemoteCallbackList<MessageReceiverListener>()
    private var mScheduledFuture: ScheduledFuture<*>? = null

    private val mMessenger: Messenger = Messenger(mMessengerHandler)

    private val mScheduledThreadPoolExecutor: ScheduledThreadPoolExecutor by lazy {
        ScheduledThreadPoolExecutor(
            1
        )
    }

    /**
     * 关系：
     * IConnectionInterface 接口
     * IConnectionInterface.Stub IConnectionInterface 的实现
     */
    private val mConnectionService: IConnectionService = object : IConnectionService.Stub() {
        override fun connect() {
            Log.d(
                TAG,
                "conncet() called ${progressInfo()}"
            )
            try {
                Thread.sleep(3000)
                // 会报错：java.lang.RuntimeException: Can't toast on a thread that has not called Looper.prepare()
                // Toast.makeText(this@RemoteService, "连接成功", Toast.LENGTH_SHORT).show()
                mMainHandler.post {
                    Toast.makeText(this@RemoteService, "连接成功", Toast.LENGTH_SHORT).show()
                }
                mIsConnected = true
                mScheduledFuture = mScheduledThreadPoolExecutor.scheduleAtFixedRate({
                    try {
                        // 模拟发送消息
                        // 已注册监听的回调
                        val count = mMessageListener.beginBroadcast()
                        // Log.d(TAG, "connect:scheduleAtFixedRate count = $count")
                        // API 17以上才能调用
                        // val callbackCount = mMessageListener.registeredCallbackCount
                        for (i in 0 until count) {
                            // Log.d(TAG, "connect:scheduleAtFixedRate $i")
                            val message = Message("Message From Remote")
                            val listener = mMessageListener.getBroadcastItem(i);
                            // API 26 以上才能调用
                            // mMessageListener.getRegisteredCallbackItem(i)

                            listener.onReceiveMessage(message)
                        }
                        // 注意，要结束
                        mMessageListener.finishBroadcast()
                    } catch (e: Exception) {
                        Log.e(TAG, "connect: scheduleAtFixedRate", e)
                    }
//                    mMessageListener.forEach {
//                        val message = Message("Message From Remote")
//                        try {
//                            it.onReceiveMessage(message)
//                        } catch (e: Exception) {
//                            Log.e(TAG, "connect: scheduleAtFixedRate", e)
//                        }
//                    }
                }, 1000, 5000, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {

            }
        }

        override fun disconnect() {
            Log.d(TAG, "disconnect() called ${progressInfo()}")
            mIsConnected = false
            // 取消消息发送
            mScheduledFuture?.cancel(true)
            mMainHandler.post {
                Toast.makeText(this@RemoteService, "断开连接", Toast.LENGTH_SHORT).show()
            }
        }

        override fun isConnected(): Boolean {
            Log.d(TAG, "isConnected() called ${progressInfo()}")
            return mIsConnected
        }
    }

    private val mMessageService = object : IMessageService.Stub() {
        override fun sendMessage(message: Message?) {
            // 如果用了关键字 in，那么这里的 message 对象就是 传递过来的，但是对于属性的修改不会同步回去，
            // 如果用了关键字 out，那么这里的 message 对象就是 Binder 新 new 出来的，而不是传递过来的
            // 但是对于属性的修改会通过 readFromParcel 同步会主进程
            // 如果用了关键字 inout，那么会同时包含 in 和 out 的特点

            Log.d(
                TAG,
                "sendMessage() called with: message = $message hashCode = ${message?.hashCode()} - ${progressInfo()}"
            )
            message?.isSendSuccess = mIsConnected
        }

        override fun registerMessageReceiveListener(listener: MessageReceiverListener?) {
            // 坑 这里注册的 listener 和 unregisterMessageReceiveListener 的 listener 不是同一个 listener
            Log.d(
                TAG,
                "registerMessageReceiveListener() called with: listener = $listener ${progressInfo()}"
            )
            listener?.apply {
//                mMessageListener.add(this)
                mMessageListener.register(this)
            }
        }

        override fun unregisterMessageReceiveListener(listener: MessageReceiverListener?) {
            Log.d(
                TAG,
                "unregisterMessageReceiveListener() called with: listener = $listener ${progressInfo()}"
            )
            listener?.apply {
//                mMessageListener.remove(this)
                mMessageListener.unregister(this)
            }
        }
    }

    private val mServiceManager = object : IServiceManager.Stub() {
        override fun getSerive(serviceName: String?): IBinder? {
            return when {
                IConnectionService::class.java.simpleName == serviceName -> {
                    mConnectionService.asBinder()
                }
                IMessageService::class.java.simpleName == serviceName -> {
                    mMessageService.asBinder()
                }
                Messenger::class.java.simpleName == serviceName -> {
                    mMessenger.binder
                }
                else -> null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RemoteService onCreate() called ${progressInfo()}")
    }

    override fun onBind(intent: Intent): IBinder? {
        return mServiceManager.asBinder()
    }

    private fun progressInfo(): String =
        " 当前进程ID： ${getProgressId()} 进程名：${this@RemoteService.progressName()} 线程：${Thread.currentThread().name}"
}