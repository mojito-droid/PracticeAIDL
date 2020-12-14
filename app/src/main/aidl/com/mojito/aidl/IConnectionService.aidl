// IConnectionService.aidl
package com.mojito.aidl;

// AIDL 连接服务
// 定义好 AIDL 文件后编译一下

interface IConnectionService {
    /**
     * AIDL 支持的基本类型,
     * 注意：这些基本类型和 Java 的语法对应
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);
    // void 会阻塞调用方，也就是说 IPC 的调用时阻塞式的
    // void connect();
    // 关键字：oneway，可以让方法不是阻塞的
    // 但是限制条件是 返回值必须是 void
    // Tip 可以尝试将返回值修改编译看一下什么错误: oneway method 'connect' cannot return a value
    oneway void connect();

    void disconnect();

    boolean isConnected();
}