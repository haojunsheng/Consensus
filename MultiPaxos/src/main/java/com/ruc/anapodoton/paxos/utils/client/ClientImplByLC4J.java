package com.ruc.anapodoton.paxos.utils.client;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.github.luohaha.client.LightCommClient;
import com.github.luohaha.connection.Conn;
import com.github.luohaha.exception.ConnectionCloseException;
import com.github.luohaha.param.ClientParam;

public class ClientImplByLC4J implements CommClient {

	private LightCommClient client;
	//用于把ip，port 和Conn进行映射
	private Map<String, Conn> addressToConn = new HashMap<>();

	public ClientImplByLC4J(int ioThreadPoolSize) {
		// TODO Auto-generated constructor stub
		try {
			this.client = new LightCommClient(ioThreadPoolSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 向目的节点发送消息
	 * @param ip
	 * @param port
	 * @param msg
	 * @throws ClosedChannelException
	 */
	@Override
	public void sendTo(String ip, int port, byte[] msg) throws ClosedChannelException {
		// 初始化ClientParam
		ClientParam param = new ClientParam();
		param.setLogLevel(Level.WARNING);
		param.setOnConnect(conn -> {
			String key = ip + ":" + port;
			this.addressToConn.put(key, conn);
		});
		String key = ip + ":" + port;
		//
		if (!addressToConn.containsKey(key)) {
			try {
				client.connect(ip, port, param);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		int count = 0;
		do {
			try {
				if (count >= 3)
					break;
				while (addressToConn.get(key) == null);
				addressToConn.get(key).write(msg);
				break;
			} catch (ConnectionCloseException e) {
				e.printStackTrace();
				addressToConn.remove(key);
				try {
					client.connect(ip, port, param);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				count++;
			}
		} while (true);
	}

}
