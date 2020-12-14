// IMessageService.aidl
package com.mojito.aidl;
// 导包必须写
import com.mojito.aidl.entity.Message;
import com.mojito.aidl.MessageReceiverListener;

// 消息服务
interface IMessageService {
    // in: 对象由 main 进程传递给 Remote 进程，Remote 进程对对象的修改，不会音响 main 进程的对象
    // out：对象由 Remote 进程传递给 main 进程，main 进程对对象的修改，不会音响 Remote 进程的对象
    // inout：对象在 main 进程和 remote 进程之间传递的时候，修改会互相影响
    // Tips: 可以一个个修改看一下会会出现什么现象
    // (PS：out 会让 message 对象为空)
    void sendMessage(inout Message message);
    void registerMessageReceiveListener(MessageReceiverListener listener);
    void unregisterMessageReceiveListener(MessageReceiverListener listener);
}