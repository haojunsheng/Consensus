package com.ruc.anapodoton.paxos.utils;

import com.ruc.anapodoton.paxos.core.ConfObject;
import com.google.gson.Gson;

public class ConfReader {
	
	/**
	 * 返回配置文件信息
	 * @param filename
	 * @return
	 */
	private static ConfObject read(String filename) {
		String data = FileUtils.readFromFile(filename);
		if (data == null || data.length() == 0) {
			System.err.println("配置文件出错 - " + filename);
		}
		return new Gson().fromJson(data, ConfObject.class);
	}
	
}
