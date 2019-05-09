package com.ruc.anapodoton.paxos.kvTest;

import java.io.IOException;

import com.ruc.anapodoton.paxos.main.MultiPaxos;

public class ServerTest2 {
	public static void main(String[] args) {
		try {
			MultiPaxos server = new MultiPaxos("./conf/conf2.json");
			server.setGroupId(1, new KvCallback());
			server.setGroupId(2, new KvCallback());
			server.start();
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
