// Message.aidl
package com.mojito.aidl.entity;
// 关联Java实体类
parcelable Message;

// 为了让 AIDL 引用 Java 类，定义的时候要注意
// 1. 包名路径要与被引用的类包名路径一致
// 2. 类名要与被引用的类名一致
