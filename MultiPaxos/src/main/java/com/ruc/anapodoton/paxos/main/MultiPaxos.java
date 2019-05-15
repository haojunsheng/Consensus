package com.ruc.anapodoton.paxos.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ruc.anapodoton.paxos.core.*;
import com.ruc.anapodoton.paxos.packet.Packet;
import com.ruc.anapodoton.paxos.utils.FileUtils;
import com.ruc.anapodoton.paxos.utils.client.ClientImplByLC4J;
import com.ruc.anapodoton.paxos.utils.client.CommClient;
import com.ruc.anapodoton.paxos.utils.serializable.ObjectSerialize;
import com.ruc.anapodoton.paxos.utils.serializable.ObjectSerializeImpl;
import com.ruc.anapodoton.paxos.utils.server.CommServer;
import com.ruc.anapodoton.paxos.utils.server.ServerImplByLC4J;
import com.ruc.anapodoton.paxos.core.Accepter;
import com.ruc.anapodoton.paxos.core.ConfObject;
import com.ruc.anapodoton.paxos.core.InfoObject;
import com.ruc.anapodoton.paxos.core.Learner;
import com.ruc.anapodoton.paxos.core.PaxosCallback;
import com.ruc.anapodoton.paxos.core.Proposer;
import com.google.gson.Gson;

/**
 *
 */
public class MultiPaxos {

	/**
	 * 全局配置文件信息
	 */
	private ConfObject confObject;

	/**
	 * 本节点的信息
	 */
	private InfoObject infoObject;

	/**
	 * 配置文件所在的位置
	 */
	private String confFile;

	private Map<Integer, PaxosCallback> groupidToCallback = new HashMap<Integer, PaxosCallback>();

	private Map<Integer, Proposer> groupidToProposer = new HashMap<Integer, Proposer>();

	private Map<Integer, Accepter> groupidToAccepter = new HashMap<Integer, Accepter>();

	private Map<Integer, Learner> groupidToLearner = new HashMap<Integer, Learner>();

	private Gson gson = new Gson();
	
	private ObjectSerialize objectSerialize = new ObjectSerializeImpl();
	
	private Logger logger = Logger.getLogger("MultiPaxos");

	/*
	 * Paxos客户端
	 */
	private CommClient client;

	/**
	 * 实例化了confObject对象（全局配置文件信息）和infoObject对象（本节点的信息）
	 * 启动了客服端
	 * @param confFile
	 * @throws IOException
	 */
	public MultiPaxos(String confFile) throws IOException {
		super();
		this.confFile = confFile;
		this.confObject = gson.fromJson(FileUtils.readFromFile(this.confFile), ConfObject.class);
		this.infoObject = getMy(this.confObject.getNodes());
		// 实例化客户端
		this.client = new ClientImplByLC4J(4);
		this.logger.setLevel(Level.WARNING);
	}
	
	/**
	 * 设置log级别
	 * @param level
	 * 级别
	 */
	public void setLogLevel(Level level) {
		this.logger.setLevel(level);
	}
	/**
	 * add handler
	 * @param handler
	 * handler
	 */
	public void addLogHandler(Handler handler) {
		this.logger.addHandler(handler);
	}

	/**
	 * 一个实例包含Accepter，Proposer，Learner和状态机四个角色
	 * 同时把groupId分别和executor，accepter，proposer和learner进行绑定
	 * 相同编号的实例构成一个paxos group
	 * @param groupId
	 * @param executor
	 */
	public void setGroupId(int groupId, PaxosCallback executor) {
		Accepter accepter = new Accepter(infoObject.getId(), confObject.getNodes(), infoObject, confObject, groupId,
				this.client);
		Proposer proposer = new Proposer(infoObject.getId(), confObject.getNodes(), infoObject, confObject.getTimeout(),
				accepter, groupId, this.client);

		Learner learner = new Learner(infoObject.getId(), confObject.getNodes(), infoObject, confObject, accepter,
				executor, groupId, this.client);
		this.groupidToCallback.put(groupId, executor);
		this.groupidToAccepter.put(groupId, accepter);
		this.groupidToProposer.put(groupId, proposer);
		this.groupidToLearner.put(groupId, learner);
	}

	/**
	 * 获得我的accepter或者proposer信息
	 * 
	 * @param infoObjects
	 * @return
	 */
	private InfoObject getMy(List<InfoObject> infoObjects) {
		for (InfoObject each : infoObjects) {
			if (each.getId() == confObject.getMyid()) {
				return each;
			}
		}
		return null;
	}

	/**
	 * 启动paxos服务器
	 * 接受来自其他节点的消息，并且进行相应的处理
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException 
	 */
	public void start() throws IOException, InterruptedException, ClassNotFoundException {
		// 启动paxos服务器
		CommServer server = new ServerImplByLC4J(this.infoObject.getPort(), 4);
		System.out.println("paxos server-" + confObject.getMyid() + " start...");
		while (true) {
			byte[] data = server.recvFrom();
			//Packet packet = gson.fromJson(new String(data), Packet.class);
			Packet packet = objectSerialize.byteArrayToObject(data, Packet.class);
			int groupId = packet.getGroupId();
			Accepter accepter = this.groupidToAccepter.get(groupId);
			Proposer proposer = this.groupidToProposer.get(groupId);
			Learner learner = this.groupidToLearner.get(groupId);
			if (accepter == null || proposer == null || learner == null) {
				return;
			}
			WorkerType workerType=packet.getWorkerType();
			switch (workerType) {
			case ACCEPTER:
				accepter.sendPacket(packet.getPacketBean());
				break;
			case PROPOSER:
				proposer.sendPacket(packet.getPacketBean());
				break;
			case LEARNER:
				learner.sendPacket(packet.getPacketBean());
				break;
			case SERVER:
				proposer.sendPacket(packet.getPacketBean());
				break;
			default:
				break;
			}
		}
	}
}
