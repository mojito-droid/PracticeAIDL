package com.mojito.aidl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.mojito.aidl.databinding.ActivityMainBinding
import com.mojito.aidl.entity.Message

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: android.os.Message) {
            super.handleMessage(msg)
            try {
                val bundle = msg.data
                bundle.classLoader = Message::class.java.classLoader
                val message = bundle.getParcelable<Message>("message")
                Log.d(
                    TAG,
                    "MainActivity handleMessage() called with: msg = $message ${progressInfo()}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "MainActivity handleMessage: ", e)
            }
        }
    }

    private lateinit var mViewBinding: ActivityMainBinding
    private lateinit var mConnectionProxy: IConnectionService
    private lateinit var mMessageProxy: IMessageService
    private lateinit var mServiceManagerProxy: IServiceManager
    // 不适用与高并发的通信，所以如果想让 IM 在单独线程通信的话，还是自定义 AIDL 吧
    private var mMessengerProxy: Messenger? = null
    private var mClientMessenger: Messenger = Messenger(mHandler)


    private val mMessageReceiverListener: MessageReceiverListener =
        object : MessageReceiverListener.Stub() {
            override fun onReceiveMessage(message: Message?) {
                Log.d(TAG, "onReceiveMessage() called with: message = $message ${progressInfo()}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        mViewBinding.btnConnect.setOnClickListener(this)
        mViewBinding.btnDisconnect.setOnClickListener(this)
        mViewBinding.btnCheckConnectStatus.setOnClickListener(this)
        mViewBinding.btnSendMessage.setOnClickListener(this)
        mViewBinding.btnRegister.setOnClickListener(this)
        mViewBinding.btnUnregister.setOnClickListener(this)
        mViewBinding.btnSendMessageByMessenger.setOnClickListener(this)
        // 自动绑定服务
        bindRemoteService()
        Log.d(TAG, "MainActivity onCreate() called ${progressInfo()}")
    }

    private val mConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // 获取 AIDL 声明的服务
            mServiceManagerProxy = IServiceManager.Stub.asInterface(service)
            mConnectionProxy =
                IConnectionService.Stub.asInterface(
                    mServiceManagerProxy.getSerive(
                        IConnectionService::class.java.simpleName
                    )
                )
            mMessageProxy =
                IMessageService.Stub.asInterface(mServiceManagerProxy.getSerive(IMessageService::class.java.simpleName))
            mMessengerProxy =
                Messenger(mServiceManagerProxy.getSerive(Messenger::class.java.simpleName))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun bindRemoteService() {
        val intent = Intent(this, RemoteService::class.java)
        bindService(intent, mConn, Context.BIND_AUTO_CREATE)
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_connect -> {
                mConnectionProxy.connect()
            }
            R.id.btn_disconnect -> {
                mConnectionProxy.disconnect()
            }
            R.id.btn_check_connect_status -> {
                val connected = mConnectionProxy.isConnected
                Toast.makeText(this, "远程服务 ${if (connected) "已连接" else "未连接"}", Toast.LENGTH_SHORT)
                    .show()
            }
            R.id.btn_send_message -> {
                val message = Message("Message from main process")
                mMessageProxy.sendMessage(message)
                Log.d(
                    TAG,
                    "onClick: 发送消息，是否成功 - ${message.isSendSuccess} hashCode - ${message.hashCode()}"
                )
            }
            R.id.btn_register -> {
                mMessageProxy.registerMessageReceiveListener(mMessageReceiverListener)
            }
            R.id.btn_unregister -> {
                mMessageProxy.unregisterMessageReceiveListener(mMessageReceiverListener)
            }
            R.id.btn_send_message_by_messenger -> {
                val message = Message("Message from main process（Messenger）")
                val data = android.os.Message.obtain()
                data.replyTo = mClientMessenger
                val bundle = Bundle();
                bundle.putParcelable("message", message)
                data.data = bundle
                mMessengerProxy?.send(data)
                Log.d(
                    TAG,
                    "onClick: 通过 Messenger 发送消息，是否成功 - ${message.isSendSuccess} hashCode - ${message.hashCode()}"
                )
            }
        }
    }

    private fun progressInfo(): String =
        " 当前进程ID： ${getProgressId()} 进程名：${this.progressName()} 线程：${Thread.currentThread().name}"
}