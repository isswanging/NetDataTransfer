package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
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
import net.listen.GetBroadcastPacket;
import net.util.NetDomain;
import net.vo.Host;

// 程序入口
public class MainWindow {
	JTable userList;
	DefaultTableModel model;
	JPopupMenu popup;
	JLabel number;

	String hostName;
	String ip;
	String userName;
	String userDomain;

	public MainWindow() {
		// 检查端口
		preCheck();
		// 建立监听
		listen();
		// 主机登录
		login();
		// 建立界面
		initUI();

		refresh();

		System.out.println("************");
	}

	private void listen() {
		// 监听广播
		new Thread(new GetBroadcastPacket()).start();
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
			NetDomain
					.addHost(new Host(userName, userDomain, ip, hostName, 1, 1));

			// 广播登录信息
			String message = userName + "@" + userDomain + "@" + hostName + "@"
					+ ip + "@0";
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
		number.setText(String.valueOf(SystemConf.hostList.size()));
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

		// 右键菜单
		popup = new JPopupMenu();
		JMenuItem sendFile = new JMenuItem("发送文件");
		JMenuItem sendFolder = new JMenuItem("发送文件夹");
		popup.add(sendFile);
		popup.add(sendFolder);

		// 刷新事件
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("refreshing......");
				refresh();
			}
		});

		// 发送文本
		send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rowIndex = userList.getSelectedRows();
				if (rowIndex.length == 0) {
					System.out.println("请选择主机");
				} else {
					for (int i = 0; i < rowIndex.length; i++) {
						Vector<?> row = (Vector<?>) model.getDataVector()
								.get(i);
						String name = (String) row.elementAt(2);
						String ip = (String) row.elementAt(3);
						System.out.println(name + " " + ip);

						sendUdpText();
					}
				}
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

		sendFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int i = userList.getSelectedRow();
				Vector<?> row = (Vector<?>) model.getDataVector().get(i);
				String name = (String) row.elementAt(2);
				String ip = (String) row.elementAt(3);
				System.out.println(name + " " + ip);
			}

		});

		sendFolder.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int i = userList.getSelectedRow();
				Vector<?> row = (Vector<?>) model.getDataVector().get(i);
				String name = (String) row.elementAt(2);
				String ip = (String) row.elementAt(3);
				System.out.println(name + " " + ip);
			}

		});

	}

	// 发送UDP数据
	private void sendUdpText() {

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
			System.out.println("端口被占用。");
			System.exit(0);
		}
		if (NetDomain.check().equals(SystemConf.FAIL)) {
			System.out.println("输入输出异常。");
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
		new MainWindow();
	}

}
