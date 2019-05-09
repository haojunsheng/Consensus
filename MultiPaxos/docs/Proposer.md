# Proposer

这个可是重头戏：

```java
//定义Proposer所处的状态
enum Proposer_State {
   READY, PREPARE, ACCEPT, FINISH
}
```



下面我们来看构造函数

```java
 /**
	 * @param id 该节点的id
	 * @param accepters 所有的节点信息
	 * @param my 本节点的信息
	 * @param timeout 超时时间
	 * @param accepter accepter的实例
	 * @param groupId 标识第几个分组
	 * @param client 前面实例化的ClientImplByLC4J
	 */
public Proposer(int id, List<InfoObject> accepters, InfoObject my, int timeout, Accepter accepter, int groupId,CommClient client)
```

在构造函数中，首先完成了上面的变量的实例化，然后开启了两个线程，一个线程不断的接受消息，另外一个线程不断的发送消息。

我们先来看recvPacket：







