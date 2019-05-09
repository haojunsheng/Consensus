package com.ruc.anapodoton.paxos.packet;

import java.io.Serializable;

import com.ruc.anapodoton.paxos.core.WorkerType;

/**
 * Packet
 */
public class Packet implements Serializable {
	private PacketBean packetBean;
	private int groupId;
	private WorkerType workerType;
	public Packet(PacketBean packetBean, int groupId, WorkerType workerType) {
		super();
		this.packetBean = packetBean;
		this.groupId = groupId;
		this.workerType = workerType;
	}
	public PacketBean getPacketBean() {
		return packetBean;
	}
	public void setPacketBean(PacketBean packetBean) {
		this.packetBean = packetBean;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public WorkerType getWorkerType() {
		return workerType;
	}
	public void setWorkerType(WorkerType workerType) {
		this.workerType = workerType;
	}
	
}
