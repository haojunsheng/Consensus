package com.ruc.anapodoton.paxos.utils.client;

import java.io.IOException;
import java.net.UnknownHostException;

public interface CommClient {
	/**
	 *
	 * @param ip
	 * @param port
	 * @param msg
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendTo(String ip, int port, byte[] msg) throws UnknownHostException, IOException;
}
