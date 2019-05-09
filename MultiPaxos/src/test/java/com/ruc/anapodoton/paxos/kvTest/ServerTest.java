package com.ruc.anapodoton.paxos.kvTest;

import java.io.IOException;

import com.ruc.anapodoton.paxos.main.MultiPaxos;

public class ServerTest {
	public static void main(String[] args) {
		try {
			MultiPaxos server = new MultiPaxos("./conf/conf.json");
			//多个group是用来运行多个实例的，因为一个实例确定一个值，多个实例确定多个值
			//一个实例里面包含Acceptor，Proposer，Learner，State machine四个角色，
			// 不同机器上的相同编号的实例称为一个group
			server.setGroupId(1, new KvCallback());
			server.setGroupId(2, new KvCallback());
			server.start();
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
