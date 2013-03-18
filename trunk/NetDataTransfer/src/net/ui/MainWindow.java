package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import net.conf.SystemConf;
import net.listen.GetBroadcastPacket;
import net.util.NetDomain;
import net.vo.Host;

// 程序入口
public class MainWindow {
	// 在线主机列表
	public static Vector<Host> hostList = new Vector<Host>();
	JTable userList;
	DefaultTableModel model;
	JLabel number;

	public MainWindow() {
		// 检查端口
		preCheck();
		// 建立监听
		listen();
		// 主机登录
		login();
		// 建立界面
		initUI();
		
	}

	private void listen() {
		// 监听广播
		new Thread(new GetBroadcastPacket()).start();
	}

	private void login() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String hostName = addr.getHostName();// 获取主机名
			String ip = addr.getHostAddress();// 获取ip地址

			Map<String, String> map = System.getenv();
			String userName = map.get("USERNAME");// 获取用户名
			String userDomain = map.get("USERDOMAIN");// 获取计算机域

			// 加入在线列表
			NetDomain.addHost(new Host(userName, userDomain, ip, hostName, 1));

			// 广播登录信息
			String message = userName + "@" + userDomain + "@" + hostName + "@"
					+ ip;
			byte[] info = message.getBytes();

			DatagramSocket broadSocket = new DatagramSocket();// 用于广播信息
			DatagramPacket broadPacket = new DatagramPacket(info, info.length,
					InetAddress.getByName(SystemConf.broadcastIP),
					SystemConf.broadcastPort);

			// 广播信息并且寻找上线主机交换信息
			NetDomain.broadcast(broadSocket, broadPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 初始化界面
	@SuppressWarnings("serial")
	private void initUI() {
		JFrame jf = new JFrame("飞鸽");
		jf.setSize(410, 350);
		jf.setVisible(true);
		jf.setResizable(false);

		// 居中
		int wide = jf.getWidth();
		int high = jf.getHeight();
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		jf.setLocation(screenWidth / 2 - wide / 2, screenHeight / 2 - high / 2);
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// 上部
		JPanel top = new JPanel();
		JPanel count = new JPanel();
		JPanel list = new JPanel();
		number = new JLabel();

		// 设置主机列表
		String[] columnNames = { "用户名", "工作组", "主机名", "IP地址" };
		Object[][] content = new Object[][] {};
		model = new DefaultTableModel(content, columnNames);
		userList = new JTable(model) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		updateHostList();
		JScrollPane jsTable = new JScrollPane(userList);
		userList.setPreferredScrollableViewportSize(new Dimension(320, 100));
		userList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		userList.setRequestFocusEnabled(false);
		jsTable.setViewportView(userList);

		// 统计部分
		JLabel label = new JLabel("联机人数:", SwingConstants.CENTER);
		number.setText(String.valueOf(hostList.size()));
		number.setHorizontalAlignment(JLabel.CENTER);
		JButton refresh = new JButton("刷新");

		count.setLayout(new BorderLayout());
		count.add(label, BorderLayout.NORTH);
		count.add(number, BorderLayout.CENTER);
		count.add(refresh, BorderLayout.SOUTH);
		list.add(jsTable);
		top.add(list);
		top.add(count);

		// 中部的文本框
		JPanel middle = new JPanel();
		JTextArea text = new JTextArea(7, 35);
		text.setLineWrap(true);
		JScrollPane jsText = new JScrollPane(text,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		middle.add(jsText, BorderLayout.CENTER);

		// 底部
		JPanel bottom = new JPanel();
		JButton send = new JButton("发送");
		bottom.add(send, BorderLayout.CENTER);

		// 总布局
		jf.setLayout(new BorderLayout());
		jf.add(top, BorderLayout.NORTH);
		jf.add(middle, BorderLayout.CENTER);
		jf.add(bottom, BorderLayout.SOUTH);

		// 事件
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("________________");
				refresh();
			}
		});
	}

	private void refresh() {
		// 清空列表
		clearTable();
		// 重新登录
		login();
		// 更新
		updateHostList();
	}

	private void clearTable() {
		
		hostList = new Vector<Host>();
		System.out.println(hostList.size());
		
		int rowCount = userList.getRowCount();
		DefaultTableModel model = (DefaultTableModel) userList.getModel();
		for (int i = 0; i < rowCount; i++) {
			model.removeRow(0);
		}
	}

	// 检查端口
	private void preCheck() {
		if (NetDomain.check().equals(SystemConf.ERROR)) {
			System.out.println("端口被占用。");
			System.exit(0);
		}
		if (NetDomain.check().equals(SystemConf.FAIL)) {
			System.out.println("输入输出异常。");
			System.exit(0);
		}
	}

	// 更新主机列表
	public void updateHostList() {
		number.setText(String.valueOf(hostList.size()));
		for (Host host : hostList) {
			model.addRow(new String[] { host.getUserName(),
					host.getGroupName(), host.getHostName(), host.getIp() });
		}
	}

	public static void main(String[] args) {
		new MainWindow();
	}

}
