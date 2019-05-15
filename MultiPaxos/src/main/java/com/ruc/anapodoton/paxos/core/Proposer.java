package com.ruc.anapodoton.paxos.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.ruc.anapodoton.paxos.utils.client.CommClient;
import com.ruc.anapodoton.paxos.utils.serializable.ObjectSerialize;
import com.ruc.anapodoton.paxos.utils.serializable.ObjectSerializeImpl;
import com.ruc.anapodoton.paxos.packet.AcceptPacket;
import com.ruc.anapodoton.paxos.packet.AcceptResponsePacket;
import com.ruc.anapodoton.paxos.packet.Packet;
import com.ruc.anapodoton.paxos.packet.PacketBean;
import com.ruc.anapodoton.paxos.packet.PreparePacket;
import com.ruc.anapodoton.paxos.packet.PrepareResponsePacket;
import com.ruc.anapodoton.paxos.packet.Value;

/**
 *
 */
public class Proposer {
	//定义Proposer所处的状态
	enum Proposer_State {
		READY, PREPARE, ACCEPT, FINISH
	}
	//一个实例确定一个值，我们需要有序的确定多个值
	class Instance {
		private int ballot;//投票编号
		// a set for promise receive
		private Set<Integer> pSet;//收到的所有的承诺
		// value found after phase 1
		private Value value;//prepare阶段之后，从所有Acceptor的Promise中选定的值
		// value's ballot
		private int valueBallot;//选定的值对应的投票编号
		// accept set
		private Set<Integer> acceptSet;//提案被Accept的集合
		// is wantvalue doneValue
		private boolean isSucc;//标识提案是否成功
		// state
		private Proposer_State state;//提案者目前所处的状态

		public Instance(int ballot, Set<Integer> pSet, Value value, int valueBallot, Set<Integer> acceptSet,
				boolean isSucc, Proposer_State state) {
			super();
			this.ballot = ballot;
			this.pSet = pSet;
			this.value = value;
			this.valueBallot = valueBallot;
			this.acceptSet = acceptSet;
			this.isSucc = isSucc;
			this.state = state;
		}

	}
	//
	private Map<Integer, Instance> instanceState = new HashMap<>();

	// current instance，一个实例确定一个值，
	private int currentInstance = 0;

	// proposer's id
	private int id;

	// accepter's number
	private int accepterNum;

	// accepters
	private List<InfoObject> accepters;

	// my info
	private InfoObject my;

	// timeout for each phase(ms)
	private int timeout;

	// 准备提交的状态
	private BlockingQueue<Value> readyToSubmitQueue = new ArrayBlockingQueue<>(1);

	// 成功提交的状态
	private BlockingQueue<Value> hasSummitQueue = new ArrayBlockingQueue<>(1);

	// 上一次的提交是否成功
	private boolean isLastSumbitSucc = false;

	// 本节点的accepter
	private Accepter accepter;

	// 组id
	private int groupId;

	// 消息队列，保存packetbean
	private BlockingQueue<PacketBean> msgQueue = new LinkedBlockingQueue<>();

	private BlockingQueue<PacketBean> submitMsgQueue = new LinkedBlockingQueue<>();

	private ObjectSerialize objectSerialize = new ObjectSerializeImpl();

	private Logger logger = Logger.getLogger("MultiPaxos");

	// 客户端
	private CommClient client;

	/**
	 *
	 * @param id 该节点的id
	 * @param accepters 所有的节点信息
	 * @param my 本节点的信息
	 * @param timeout 超时时间
	 * @param accepter accepter的实例
	 * @param groupId 标识第几个分组
	 * @param client 前面实例化的ClientImplByLC4J
	 */
	public Proposer(int id, List<InfoObject> accepters, InfoObject my, int timeout, Accepter accepter, int groupId,
			CommClient client) {
		this.id = id;
		this.accepters = accepters;
		this.accepterNum = accepters.size();
		this.my = my;
		this.timeout = timeout;
		this.accepter = accepter;
		this.groupId = groupId;
		this.client = client;
		new Thread(() -> {
			while (true) {
				try {
					//一直接受消息
					PacketBean msg = this.msgQueue.take();
					recvPacket(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() -> {
			while (true) {
				try {
					//不断的发送消息
					PacketBean msg = this.submitMsgQueue.take();
					submit((Value) msg.getData());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * 向消息队列中插入packetbean
	 * 
	 * @param bean
	 * @throws InterruptedException
	 */
	public void sendPacket(PacketBean bean) throws InterruptedException {
		this.msgQueue.put(bean);
	}

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

	/**
	 * 客户端向proposer提交想要提交的状态
	 * 
	 * @param object
	 * @return
	 * @throws InterruptedException
	 */
	public Value submit(Value object) throws InterruptedException {
		this.readyToSubmitQueue.put(object);
		beforPrepare();
		Value value = this.hasSummitQueue.take();
		return value;
	}

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
			// multi-paxos 中的优化，直接发起accept
			instance.isSucc = true;
			accept(this.id, this.currentInstance, 1, this.readyToSubmitQueue.peek());
		}
	}

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

	/**
	 * 向所有的accepter发送accept，并设置状态。
	 * 
	 * @param id
	 * @param instance
	 * @param ballot
	 * @param value
	 */
	private void accept(int id, int instance, int ballot, Value value) {
		this.instanceState.get(instance).state = Proposer_State.ACCEPT;
		try {
			PacketBean bean = new PacketBean("AcceptPacket", new AcceptPacket(id, instance, ballot, value));
			byte[] msg = this.objectSerialize.objectToObjectArray(new Packet(bean, groupId, WorkerType.ACCEPTER));
			this.accepters.forEach((info) -> {
				try {
					this.client.sendTo(info.getHost(), info.getPort(), msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		setTimeout(new TimerTask() {
			@Override
			public void run() {
				// 如果没有被Accept，那么需要重新发起prepare请求
				Instance current = instanceState.get(instance);
				if (current.state == Proposer_State.ACCEPT) {
					current.ballot++;
					prepare(id, instance, current.ballot);
				}
			}
		});
	}

	/**
	 * 接收到accepter返回的accept响应
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

	/**
	 * 本次paxos选举结束
	 */
	public void done(int instance) {
		this.instanceState.get(instance).state = Proposer_State.FINISH;
	}

	/**
	 * set timeout task
	 * 
	 * @param task
	 */
	private void setTimeout(TimerTask task) {
		new Timer().schedule(task, this.timeout);
	}
}