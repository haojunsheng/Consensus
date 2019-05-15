package com.ruc.anapodoton.paxos.packet;

import java.io.Serializable;

/**
 * Prepare阶段收到的响应的内容
 */
public class PrepareResponsePacket implements Serializable {
	private int id;//表示哪个节点
	private int instance;//实例的编号
	private boolean ok;//标识节点是否响应Proposer
	private int ab;//means accept ballot,接受的投票编号
	private Value av;//means accept value,接受的提案的值
	public PrepareResponsePacket(int id, int instance, boolean ok, int ab, Value av) {
		super();
		this.id = id;
		this.instance = instance;
		this.ok = ok;
		this.ab = ab;
		this.av = av;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getInstance() {
		return instance;
	}
	public void setInstance(int instance) {
		this.instance = instance;
	}
	public boolean isOk() {
		return ok;
	}
	public void setOk(boolean ok) {
		this.ok = ok;
	}
	public int getAb() {
		return ab;
	}
	public void setAb(int ab) {
		this.ab = ab;
	}
	public Value getAv() {
		return av;
	}
	public void setAv(Value av) {
		this.av = av;
	}
	
}
