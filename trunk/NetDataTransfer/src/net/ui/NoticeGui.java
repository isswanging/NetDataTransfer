package net.ui;

import java.awt.Component;

import javax.swing.JOptionPane;

public class NoticeGui {

	public static void warnNotice(Component comp, String content) {
		JOptionPane.showMessageDialog(comp, content, "提示",
				JOptionPane.WARNING_MESSAGE);
	}

	public static void errorNotice(Component comp, String content) {
		JOptionPane.showMessageDialog(comp, content, "错误",
				JOptionPane.ERROR_MESSAGE);
	}

}
