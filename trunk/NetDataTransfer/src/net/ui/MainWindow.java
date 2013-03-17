package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.conf.SystemConf;
import net.util.NetDomain;

// 主界面，程序入口
public class MainWindow {
	int hostNumber = 1;

	public MainWindow() {
		// 检查端口
		preCheck();
		// 启动监听线程
		listen();
		// 登录系统
		login();
		// 初始化界面
		initUI();

	}

	private void listen() {

	}

	private void login() {

	}

	private void initUI() {
		JFrame jf = new JFrame("飞鸽");
		jf.setSize(410, 360);
		jf.setVisible(true);
		jf.setResizable(false);

		// 居中显示
		int wide = jf.getWidth();
		int high = jf.getHeight();
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		jf.setLocation(screenWidth / 2 - wide / 2, screenHeight / 2 - high / 2);
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// 上部面板
		JPanel top = new JPanel();
		JPanel count = new JPanel();
		JPanel list = new JPanel();
		JLabel number = new JLabel();

		// 主机用户列表
		String[] columnNames = { "用户名", "工作组", "主机名", "IP地址" };
		Object[][] content = new Object[][] {};
		JTable userList = new JTable(content, columnNames);
		JScrollPane jsTable = new JScrollPane(userList);
		userList.setPreferredScrollableViewportSize(new Dimension(320, 100));
		userList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		userList.setRequestFocusEnabled(false);
		jsTable.setViewportView(userList);

		// 统计
		JLabel label = new JLabel("主机数:", SwingConstants.CENTER);
		number.setText(String.valueOf(hostNumber));
		number.setHorizontalAlignment(JLabel.CENTER);
		JButton refresh = new JButton("刷新");

		count.setLayout(new BorderLayout());
		count.add(label, BorderLayout.NORTH);
		count.add(number, BorderLayout.CENTER);
		count.add(refresh, BorderLayout.SOUTH);
		list.add(jsTable);
		top.add(list);
		top.add(count);

		// 中部输入框
		JPanel middle = new JPanel();
		JTextArea text = new JTextArea(7, 35);
		text.setLineWrap(true);
		JScrollPane jsText = new JScrollPane(text,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		middle.add(jsText, BorderLayout.CENTER);

		// 下部面板
		JPanel bottom = new JPanel();
		JButton send = new JButton("发送");
		bottom.add(send, BorderLayout.CENTER);

		// 整体布局
		jf.setLayout(new BorderLayout());
		jf.add(top, BorderLayout.NORTH);
		jf.add(middle, BorderLayout.CENTER);
		jf.add(bottom, BorderLayout.SOUTH);
	}

	private void preCheck() {
		// 如果端口被占用则退出
		if (NetDomain.check().equals(SystemConf.ERROR)) {
			System.out.println("通信端口被占用");
			System.exit(0);
		}
		if (NetDomain.check().equals(SystemConf.FAIL)) {
			System.out.println("输入输出错误");
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		new MainWindow();
	}

}
