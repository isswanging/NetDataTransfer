package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.MouseInputListener;
import javax.swing.table.DefaultTableModel;

import net.conf.SystemConf;
import net.listen.BroadcastMonitor;
import net.listen.FileMonitor;
import net.listen.UdpDataMonitor;
import net.util.NetDomain;
import net.vo.DataPacket;
import net.vo.Host;

// 程序入口
public class MainGui {
	JTable userList;
	DefaultTableModel model;
	JPopupMenu popup;
	JLabel number;
	JTextArea text;
	JFrame jf;

	String hostName;
	String ip;
	String userName;
	String userDomain;

	public MainGui() {
		// 检查端口
		preCheck();
		// 建立监听
		listen();
		// 主机登录
		login();
		// 建立界面
		initUI();

		System.out.println("************");
	}

	private void listen() {
		// 监听广播
		new Thread(new BroadcastMonitor()).start();
		new Thread(new UdpDataMonitor()).start();
		new Thread(new FileMonitor()).start();
	}

	private void login() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostName = addr.getHostName();// 获取主机名
			ip = addr.getHostAddress();// 获取ip地址

			Map<String, String> map = System.getenv();
			userName = map.get("USERNAME");// 获取用户名
			userDomain = map.get("USERDOMAIN");// 获取计算机域

			// 加入在线列表
			Host host = new Host(userName, userDomain, ip, hostName, 1, 0);
			NetDomain.addHost(host);

			// 广播登录信息
			NetDomain.sendUdpData(new DatagramSocket(), host,
					SystemConf.broadcastIP, SystemConf.broadcastPort);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 初始化界面
	@SuppressWarnings("serial")
	private void initUI() {
		jf = new JFrame("飞鸽");
		jf.setSize(450, 380);
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
		
		JScrollPane jsTable = new JScrollPane(userList);
		userList.setPreferredScrollableViewportSize(new Dimension(340, 120));
		userList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		userList.setRequestFocusEnabled(false);
		jsTable.setViewportView(userList);
		
		// 统计部分
		JLabel label = new JLabel("联机人数:", SwingConstants.CENTER);
		// number.setText(String.valueOf(SystemConf.hostList.size()));
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
		text = new JTextArea(7, 37);
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

		// 右键菜单
		popup = new JPopupMenu();
		JMenuItem sendFile = new JMenuItem("发送文件");
		// JMenuItem sendFolder = new JMenuItem("发送文件夹");
		popup.add(sendFile);
		// popup.add(sendFolder);
		
		// 刷新事件
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("refreshing......");
				refresh();
			}
		});

		// JTable上的右键菜单
		MouseInputListener mil = new MouseInputListener() {
			public void mouseClicked(MouseEvent e) {
				processEvent(e);
			}

			public void mousePressed(MouseEvent e) {
				processEvent(e);
			}

			public void mouseReleased(MouseEvent e) {
				processEvent(e);
				if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0
						&& !e.isControlDown() && !e.isShiftDown()) {
					popup.show(userList, e.getX(), e.getY());
				}
			}

			public void mouseEntered(MouseEvent e) {
				processEvent(e);
			}

			public void mouseExited(MouseEvent e) {
				processEvent(e);
			}

			public void mouseDragged(MouseEvent e) {
				processEvent(e);
			}

			public void mouseMoved(MouseEvent e) {
				processEvent(e);
			}

			private void processEvent(MouseEvent e) {
				if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
					int modifiers = e.getModifiers();
					modifiers -= MouseEvent.BUTTON3_MASK;
					modifiers |= MouseEvent.BUTTON1_MASK;
					MouseEvent ne = new MouseEvent(e.getComponent(), e.getID(),
							e.getWhen(), modifiers, e.getX(), e.getY(),
							e.getClickCount(), false);
					userList.dispatchEvent(ne);
				}
			}

		};
		userList.addMouseListener(mil);
		userList.addMouseMotionListener(mil);
		
		// 发送文字
		send.addActionListener(new SendText());

		// 发送文件
		sendFile.addActionListener(new SendFile());
		
		// 填充JTable
		updateHostList();

	}

	// 发送UDP数据
	private void sendUdpData(String hostName, String ip, String targetIp,
			String message, int tag, int port) {
		DataPacket dp = new DataPacket(ip, hostName, message, tag);
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			NetDomain.sendUdpData(socket, dp, targetIp, port);
		} catch (SocketException e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}
	}

	private void refresh() {
		// 清空列表
		clearTable();
		// 重新登录
		login();
		// 更新(延时一点，等待网络通信)
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		updateHostList();
	}

	private void clearTable() {
		System.out.println("before clear" + SystemConf.hostList.size());
		SystemConf.hostList.clear();// = new Vector<Host>();
		// System.out.println(SystemConf.hostList.size());

		int rowCount = userList.getRowCount();
		DefaultTableModel model = (DefaultTableModel) userList.getModel();
		for (int i = 0; i < rowCount; i++) {
			model.removeRow(0);
		}
	}

	// 检查端口
	private void preCheck() {
		if (NetDomain.check().equals(SystemConf.ERROR)) {
			NoticeGui.errorNotice(jf, "端口被占用");
			System.exit(0);
		}
		if (NetDomain.check().equals(SystemConf.FAIL)) {
			NoticeGui.errorNotice(jf, "IO异常");
			System.exit(0);
		}
	}

	// 更新主机列表
	private void updateHostList() {
		System.out.println("table size"
				+ String.valueOf(SystemConf.hostList.size()));
		number.setText(String.valueOf(SystemConf.hostList.size()));
		for (Host host : SystemConf.hostList) {
			model.addRow(new String[] { host.getUserName(),
					host.getGroupName(), host.getHostName(), host.getIp() });
		}
	}

	public static void main(String[] args) {
		new MainGui();
	}

	private class SendText implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] rowIndex = userList.getSelectedRows();
			if (rowIndex.length == 0) {
				NoticeGui.warnNotice(jf, "请选择主机");
			} else {
				String message = text.getText();
				if (message.equals("")) {
					NoticeGui.warnNotice(jf, "请输入消息");
				} else {
					for (int i = 0; i < rowIndex.length; i++) {
						Vector<?> row = (Vector<?>) model.getDataVector().get(
								rowIndex[i]);
						String targetIp = (String) row.elementAt(3);
						sendUdpData(hostName, ip, targetIp, message, 0,
								SystemConf.textPort);
					}
				}
			}

		}

	}

	private class SendFile implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			int i = userList.getSelectedRow();
			Vector<?> row = (Vector<?>) model.getDataVector().get(i);
			String targetIp = (String) row.elementAt(3);

			if (targetIp.equals(MainGui.this.ip)) {
				NoticeGui.warnNotice(jf, "不自己给自己发文件");
			} else {
				// 选择文件
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setMultiSelectionEnabled(true);
				jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

				if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
					String path = jFileChooser.getSelectedFile().getPath();
					System.out.println(path);

					// 发送确认消息
					sendUdpData(hostName, ip, targetIp, path, 1,
							SystemConf.textPort);

				}
			}

		}

	}

}
