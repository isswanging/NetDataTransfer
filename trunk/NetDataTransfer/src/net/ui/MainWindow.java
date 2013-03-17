package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;

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
import net.vo.Host;

// �����棬�������
public class MainWindow {
	// �����б�
	ArrayList<Host> hostList = new ArrayList<Host>();
	// ������
	int hostNumber = 1;

	public MainWindow() {
		// ���˿�
		preCheck();
		// ���������߳�
		listen();
		// ��¼ϵͳ
		login();
		// ��ʼ������
		initUI();

	}

	private void listen() {

	}

	private void login() {
		// 广播登录信息
		Host host = NetDomain.getHost();
	}

	private void initUI() {
		JFrame jf = new JFrame("�ɸ�");
		jf.setSize(410, 360);
		jf.setVisible(true);
		jf.setResizable(false);

		// ������ʾ
		int wide = jf.getWidth();
		int high = jf.getHeight();
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		jf.setLocation(screenWidth / 2 - wide / 2, screenHeight / 2 - high / 2);
		jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		// �ϲ����
		JPanel top = new JPanel();
		JPanel count = new JPanel();
		JPanel list = new JPanel();
		JLabel number = new JLabel();

		// �����û��б�
		String[] columnNames = { "�û���", "������", "������", "IP��ַ" };
		Object[][] content = new Object[][] {};
		JTable userList = new JTable(content, columnNames);
		JScrollPane jsTable = new JScrollPane(userList);
		userList.setPreferredScrollableViewportSize(new Dimension(320, 100));
		userList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		userList.setRequestFocusEnabled(false);
		jsTable.setViewportView(userList);

		// ͳ��
		JLabel label = new JLabel("��������:", SwingConstants.CENTER);
		number.setText(String.valueOf(hostNumber));
		number.setHorizontalAlignment(JLabel.CENTER);
		JButton refresh = new JButton("ˢ��");

		count.setLayout(new BorderLayout());
		count.add(label, BorderLayout.NORTH);
		count.add(number, BorderLayout.CENTER);
		count.add(refresh, BorderLayout.SOUTH);
		list.add(jsTable);
		top.add(list);
		top.add(count);

		// �в������
		JPanel middle = new JPanel();
		JTextArea text = new JTextArea(7, 35);
		text.setLineWrap(true);
		JScrollPane jsText = new JScrollPane(text,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		middle.add(jsText, BorderLayout.CENTER);

		// �²����
		JPanel bottom = new JPanel();
		JButton send = new JButton("����");
		bottom.add(send, BorderLayout.CENTER);

		// ���岼��
		jf.setLayout(new BorderLayout());
		jf.add(top, BorderLayout.NORTH);
		jf.add(middle, BorderLayout.CENTER);
		jf.add(bottom, BorderLayout.SOUTH);
	}

	private void preCheck() {
		// ���˿ڱ�ռ�����˳�
		if (NetDomain.check().equals(SystemConf.ERROR)) {
			System.out.println("ͨ�Ŷ˿ڱ�ռ��");
			System.exit(0);
		}
		if (NetDomain.check().equals(SystemConf.FAIL)) {
			System.out.println("�����������");
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		new MainWindow();
	}

}
