# Proposer

这个可是重头戏：

MultiPaxos中的setGroupId(int groupId, PaxosCallback executor)函数，实例化了Proposer，在Proposer的构造函数中，初始化了一些成员变量，然后开启了两个线程，一个线程不断的接受消息recvPacket(msg)，另外一个线程不断的发送消息submit((Value) msg.getData())。在recvPacket(msg)中，根据消息的类型onPrepareResponse(int peerId, int instance, boolean ok, int ab, Value av)

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

我们先来看**submit**，用于发送提案，

submit中调用了beforPrepare()，我们在看prepare之前需要先看下Instance的定义，

```java
class Instance {
   private int ballot;//投票编号
   // a set for promise receive
   private Set<Integer> pSet;//收到的所有的承诺
   // value found after phase 1
   private Value value;//prepare阶段之后，从所有Acceptor的Promise中选定的值
   // value's ballot
   private int valueBallot;//选定的值对应的投票编号
   // accept set
   private Set<Integer> acceptSet;//提案是否被最终Accept
   // is wantvalue doneValue
   private boolean isSucc;//是否成功
   // state
   private Proposer_State state;//提案者目前所处的状态
   }
```

我们来看beforPrepare的具体实现，首先是初始化了instanceState，然后判断该节点是不是leader，如果是，直接发起accept请求，如果不是，则发起prepare请求。

```java
/**
 * 为了优化提案成功的效率，如果我之前的提案被AC了，那么我就是leader，提案不用经过prepare，直接Accept阶段就可以了
 */
public void beforPrepare() {
   // 获取accepter最近的一次instance的id，Acceptor记录一个全局的最大提案编号
   this.currentInstance = Math.max(this.currentInstance, accepter.getLastInstanceId());
   this.currentInstance++;
   Instance instance = new Instance(1, new HashSet<>(), null, 0, new HashSet<>(), false, Proposer_State.READY);
   this.instanceState.put(this.currentInstance, instance);
   //上一次的提交是否成功
   if (this.isLastSumbitSucc == false) {
      // 执行完整的流程
      prepare(this.id, this.currentInstance, 1);
   } else {
      // multi-paxos 中的优化，直接accept
      instance.isSucc = true;
      accept(this.id, this.currentInstance, 1, this.readyToSubmitQueue.peek());
   }
}
```

我们看下prepare请求,首先修改当前instance的状态，然后把要发送的消息进行拼装，通过RPC发送给其他的节点。如果超时，则检查prepare是否完成，如果没有，则重试。

```java
/**
 * 将prepare发送给所有的accepter，并设置超时。
 * 如果超时，则判断阶段1是否完成，如果未完成，则ballot加一之后继续执行阶段一。
 * 
 * 并且封装相应的消息
 * @param instance
 *            current instance
 * @param ballot
 *            prepare's ballot
 */
private void prepare(int id, int instance, int ballot) {
   //修改当前instance的状态为PREPARE
   this.instanceState.get(instance).state = Proposer_State.PREPARE;
   try {
      PacketBean bean = new PacketBean("PreparePacket", new PreparePacket(id, instance, ballot));
      byte[] msg = this.objectSerialize.objectToObjectArray(new Packet(bean, groupId, WorkerType.ACCEPTER));
      this.accepters.forEach((info) -> {
         try {
            this.client.sendTo(info.getHost(), info.getPort(), msg);
         } catch (IOException e) {
            e.printStackTrace();
         }
      });
   } catch (IOException e) {
      e.printStackTrace();
   }
   setTimeout(new TimerTask() {
      @Override
      public void run() {
         // retry phase 1 again!
         Instance current = instanceState.get(instance);
         if (current.state == Proposer_State.PREPARE) {
            current.ballot++;
            prepare(id, instance, current.ballot);
         }
      }
   });
}
```

下面我们看下accept阶段，和prepare类似，所不同的是，如果超时，则会重新发起prepare请求，而不是Accept请求。详细代码不再贴出。



我们在来看**recvPacket**：

```java
/**
	 * 处理接收到的packetbean
	 * 根据消息的类型进行处理
	 * @param bean
	 * @throws InterruptedException
	 */
	public void recvPacket(PacketBean bean) throws InterruptedException {
		switch (bean.getType()) {
		case "PrepareResponsePacket"://Prepare阶段的响应进行处理
			PrepareResponsePacket prepareResponsePacket = (PrepareResponsePacket) bean.getData();
			onPrepareResponse(prepareResponsePacket.getId(), prepareResponsePacket.getInstance(),
					prepareResponsePacket.isOk(), prepareResponsePacket.getAb(), prepareResponsePacket.getAv());
			break;
		case "AcceptResponsePacket"://Accept阶段的响应的处理
			AcceptResponsePacket acceptResponsePacket = (AcceptResponsePacket) bean.getData();
			onAcceptResponce(acceptResponsePacket.getId(), acceptResponsePacket.getInstance(),
					acceptResponsePacket.isOk());
			break;
		case "SubmitPacket"://把数据写入到磁盘
			this.submitMsgQueue.add(bean);
			break;
		default:
			System.out.println("unknown type!!!");
			break;
		}
	}
```

从上面可以看出，recvPacket根据消息的不同类型进行处理，当消息值为PrepareResponsePacket时，调用onPrepareResponse(int peerId, int instance, boolean ok, int ab, Value av) ，当消息值为AcceptResponsePacket时，调用onAcceptResponce(int peerId, int instance, boolean ok)进行处理，当消息值为SubmitPacket时，表示把数据包写入到磁盘。



下面来研究onPrepareResponse,这个的牵扯比较多，我们先把用到的相关定义列出来：

```java
/**
 * Prepare阶段收到的响应的内容
 */
public class PrepareResponsePacket implements Serializable {
	private int id;//表示哪个节点
	private int instance;//实例的编号
	private boolean ok;//标识节点是否响应Proposer
	private int ab;//means accept ballot,接受的投票编号
	private Value av;//means accept value,接受的提案的值
}
```

我们来看onPrepareResponse的实现：

```java
/**
 * 接收到accepter对于prepare的回复
 * 
 * @param peerId 表示哪个节点
 * @param instance 实例的编号
 * @param ok 是否接受了本次的提案
 * @param ab
 * @param av
 * @throws InterruptedException
 */
public void onPrepareResponse(int peerId, int instance, boolean ok, int ab, Value av) {
   Instance current = this.instanceState.get(instance);
   if (current.state != Proposer_State.PREPARE)
      return;
   if (ok) {
      //保存所有回复的节点的ID
      current.pSet.add(peerId);
      //如果提案编号小于Acceptor返回的提案编号，则更新当前提案编号
      if (ab > current.valueBallot && av != null) {
         current.valueBallot = ab;
         current.value = av;
         current.isSucc = false;
      }
      //如果获取到大多数节点的回应，则发起Accept请求
      if (current.pSet.size() >= this.accepterNum / 2 + 1) {
         if (current.value == null) {
            Value object = this.readyToSubmitQueue.peek();
            current.value = object;
            current.isSucc = true;
         }
         accept(id, instance, current.ballot, current.value);
      }
   }
}
```

Proposer得到了多数Acceptor的承诺后，如果没有发现有一个Acceptor接受过一个值，那么向所有的Acceptor发起自己的值和提议编号n，否则，从所有接受过的值中选择对应的提议编号最大的，作为提议的值，提议编号仍然为n。

我们再来看onAcceptResponce，在看之前，我们需要看下AcceptResponsePacket，这个比较简单，不再贴出代码，就三个字段，分别表示节点的id，实例的ID和是否接受提案。

```java
/**
 * 接收到accepter返回的accept响应
 * 
 * @param peerId
 * @param instance
 * @param ok
 * @throws InterruptedException
 */
public void onAcceptResponce(int peerId, int instance, boolean ok) throws InterruptedException {
   Instance current = this.instanceState.get(instance);
   if (current.state != Proposer_State.ACCEPT)
      return;
   if (ok) {
      current.acceptSet.add(peerId);
      if (current.acceptSet.size() >= this.accepterNum / 2 + 1) {
         // 流程结束
         done(instance);
         if (current.isSucc) {
            this.isLastSumbitSucc = true;
            this.hasSummitQueue.put(this.readyToSubmitQueue.take());
         } else {
            // 说明这个instance的id已经被占有
            this.isLastSumbitSucc = false;
            beforPrepare();
         }
      }
   }
}
```































