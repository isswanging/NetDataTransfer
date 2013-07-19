package net.vo;

import java.io.Serializable;

// 数据包
public class DataPacket implements Serializable {
	private static final long serialVersionUID = -4196219561242601831L;
	
	private String ip;
	private String senderName;
	private String content;
	private int tag;

	public DataPacket(String ip, String senderName, String content, int tag) {
		this.ip = ip;
		this.senderName = senderName;
		this.content = content;
		this.tag = tag;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
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

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

}
