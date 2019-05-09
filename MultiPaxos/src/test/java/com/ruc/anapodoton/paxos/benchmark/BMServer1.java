package com.ruc.anapodoton.paxos.benchmark;

import java.io.IOException;
import com.ruc.anapodoton.paxos.main.MultiPaxos;

public class BMServer1 {
	public static void main(String[] args) {
		try {
			MultiPaxos server = new MultiPaxos("./conf/conf.json");
			server.setGroupId(1, new BMCallback());
			server.start();
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
