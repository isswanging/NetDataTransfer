package net.vo;

// 数据包
public class DataPacket {

	private String ip; // 自己的ip
	private String senderName; // 自己的主机名
	private String content; // 发送内容
	private int tag; // 标识发送的阶段
	
	public DataPacket(){
	}

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
