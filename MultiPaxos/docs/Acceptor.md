# Acceptor

这个也是重头戏。

MultiPaxos中的setGroupId(int groupId, PaxosCallback executor)函数，实例化了Accepter，在Accepter的构造函数中，初始化了一些成员变量，然后开启了两个线程，一个线程不断的接受消息recvPacket(msg)，另外一个线程不断的发送消息submit((Value) msg.getData())。在recvPacket(msg)中，根据消息的类型onPrepareResponse(int peerId, int instance, boolean ok, int ab, Value av)







