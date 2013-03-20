package net.vo;

import java.io.Serializable;

// 数据包
public class DataPacket implements Serializable {
	private static final long serialVersionUID = 8280888121375940006L;

	private String ip;
	private String senderName;
	private Object content;

	public DataPacket(String ip, String senderName, Object content) {
		this.ip = ip;
		this.senderName = senderName;
		this.content = content;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

}
