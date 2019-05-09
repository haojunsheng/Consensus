package com.ruc.anapodoton.paxos.benchmark;

import java.io.IOException;

import com.ruc.anapodoton.paxos.exception.PaxosClientNullAddressException;
import com.ruc.anapodoton.paxos.main.MultiPaxosClient;

public class BMClient {
	public static void main(String[] args) {
		MultiPaxosClient client;
		try {
			client = new MultiPaxosClient();
			client.setSendBufferSize(2000);
			client.setRemoteAddress("localhost", 33333);
			System.out.println("[START] : " + System.currentTimeMillis());
			for (int i = 1; i <= 100000; i++) {
				client.submit(String.valueOf(i).getBytes(), 1);
			}
			client.submit("end".getBytes(), 1);
			client.flush(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PaxosClientNullAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
