package net.ui;

import javax.swing.*;
import java.awt.*;

public class NoticeGui {

	public static void warnNotice(Component comp, String content) {
		JOptionPane.showMessageDialog(comp, content, "提示",
				JOptionPane.WARNING_MESSAGE);
	}

	public static void errorNotice(Component comp, String content) {
		JOptionPane.showMessageDialog(comp, content, "错误",
				JOptionPane.ERROR_MESSAGE);
	}

	public static void messageNotice(Component comp, String content) {
		JOptionPane.showMessageDialog(comp, content, "消息",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
