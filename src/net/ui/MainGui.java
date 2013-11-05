package net.ui;

import net.conf.SystemConf;
import net.listen.BroadcastMonitor;
import net.listen.FileMonitor;
import net.listen.FolderMonitor;
import net.listen.UdpDataMonitor;
import net.util.FolderPath;
import net.util.NetDomain;
import net.util.OSUtil;
import net.vo.DataPacket;
import net.vo.Host;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Vector;

// 程序入口
public class MainGui {
    JTable userList;
    DefaultTableModel model;
    JPopupMenu popup;
    JLabel number;
    JTextArea text;
    JFrame jf;
    TrayIcon trayIcon; // 托盘图标
    SystemTray tray; // 本操作系统托盘的实例

    String hostName;
    String ip;
    String userName;
    String userDomain;

    private final Log logger = LogFactory.getLog(this.getClass());

    public MainGui() {
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
        new Thread(new BroadcastMonitor()).start();
        new Thread(new UdpDataMonitor()).start();
        new Thread(new FileMonitor()).start();
        new Thread(new FolderMonitor()).start();
    }

    private void login() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();// 获取主机名
            ip = OSUtil.getLocalIP();// 获取ip地址

            Map<String, String> map = System.getenv();
            userName = map.get("USERNAME");// 获取用户名
            userDomain = map.get("USERDOMAIN");// 获取计算机域

            // 加入在线列表
            Host host = new Host(userName, userDomain, ip, hostName, 1, 0);
            NetDomain.addHost(host);

            logger.info("主机" + ip + "登录成功");

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
        // jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // 上部
        JPanel top = new JPanel();
        JPanel count = new JPanel();
        JPanel list = new JPanel();
        number = new JLabel();

        // 设置主机列表
        String[] columnNames = {"用户名", "工作组", "主机名", "IP地址"};
        Object[][] content = new Object[][]{};
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
        jf.pack();

        // 右键菜单
        popup = new JPopupMenu();
        JMenuItem sendFile = new JMenuItem("发送文件");
        JMenuItem sendFolder = new JMenuItem("发送文件夹");
        popup.add(sendFile);
        popup.add(sendFolder);

        // 填充JTable
        updateHostList();

        // 刷新事件
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                logger.info("refreshing......");
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

        // 发送文件夹
        sendFolder.addActionListener(new SendFolder());

        // 系统托盘
        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray(); // 获得本操作系统托盘的实例
            ImageIcon icon = new ImageIcon(this.getClass().getResource("owl.png")); // 将要显示到托盘中的图标
            PopupMenu pop = new PopupMenu(); // 构造一个右键弹出式菜单
            final MenuItem show = new MenuItem("open");
            final MenuItem exit = new MenuItem("exit");
            pop.add(show);
            pop.add(exit);
            trayIcon = new TrayIcon(icon.getImage(), "飞鸽Java", pop);// 实例化托盘图标
            trayIcon.setImageAutoSize(true);

            try {
                tray.add(trayIcon); // 将托盘图标添加到系统的托盘实例中
            } catch (AWTException ex) {
                ex.printStackTrace();
            }

            // 托盘图标操作
            trayIcon.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)// 鼠标双击图标
                    {
                        jf.setExtendedState(JFrame.NORMAL);// 设置状态为正常
                        jf.setVisible(true);// 显示主窗体
                    }
                }
            });

            // 托盘选项注册事件
            ActionListener trayAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // 退出程序
                    if (e.getSource() == exit) {
                        System.exit(0);// 退出程序
                    }
                    // 打开程序
                    if (e.getSource() == show) {
                        jf.setExtendedState(JFrame.NORMAL);// 设置状态为正常
                        jf.setVisible(true);
                    }
                }
            };
            show.addActionListener(trayAction);
            exit.addActionListener(trayAction);

            // 窗体最小化事件
            jf.addWindowListener(new WindowAdapter() {
                public void windowIconified(WindowEvent e) {
                    jf.setVisible(false);// 使窗口不可视
                    jf.dispose();// 释放当前窗体资源
                }
            });
        } else {
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }
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
            if (socket != null)
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
        SystemConf.hostList.clear();// = new Vector<Host>();

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
            logger.error("端口被占用");
            System.exit(0);
        }
        if (NetDomain.check().equals(SystemConf.FAIL)) {
            NoticeGui.errorNotice(jf, "IO异常");
            logger.error("IO异常！");
            System.exit(0);
        }

        // 获取本机IP
        logger.info("端口正常！");
        SystemConf.hostIP = OSUtil.getLocalIP();
    }

    // 更新主机列表
    private void updateHostList() {
        logger.debug("table size:" + String.valueOf(SystemConf.hostList.size()));
        number.setText(String.valueOf(SystemConf.hostList.size()));
        for (Host host : SystemConf.hostList) {
            model.addRow(new String[]{host.getUserName(),
                    host.getGroupName(), host.getHostName(), host.getIp()});
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
                NoticeGui.warnNotice(jf, "不需要自己给自己发文件");
            } else {
                // 选择文件
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setMultiSelectionEnabled(true);
                jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
                    String path = jFileChooser.getSelectedFile().getPath();
                    logger.debug(path);

                    // 发送确认消息
                    sendUdpData(hostName, ip, targetIp, path,
                            SystemConf.filePre, SystemConf.textPort);
                }
            }
        }
    }

    private class SendFolder implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = userList.getSelectedRow();
            Vector<?> row = (Vector<?>) model.getDataVector().get(i);
            String targetIp = (String) row.elementAt(3);

            if (targetIp.equals(MainGui.this.ip)) {
                NoticeGui.warnNotice(jf, "不需要自己给自己发文件");
            } else {
                //选择文件夹
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setMultiSelectionEnabled(true);
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
                    String p = jFileChooser.getSelectedFile().getPath();
                    FolderPath fPath = new FolderPath(p);
                    StringBuilder path = new StringBuilder(p).append("|");

                    for (File f : fPath.getFolders()) {
                        path.append(f.getPath() + "|");
                    }

                    // 记录文件总的大小
                    long total = 0;
                    FileInputStream fis;
                    try {
                        for (File f : fPath.getFiles()) {
                            path.append(f.getPath() + "*");
                            fis = new FileInputStream(f.getPath());
                            total += fis.available();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    sendUdpData(String.valueOf(total), ip, targetIp, path.toString(),
                            SystemConf.folderPre, SystemConf.textPort);
                }
            }
        }
    }

}
