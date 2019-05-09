package com.ruc.anapodoton.paxos.utils.server;

public interface CommServer {
	public byte[] recvFrom() throws InterruptedException;
}
