package net.util;

import java.io.File;
import java.util.ArrayList;

public class FolderPath {
	ArrayList<File> folders = new ArrayList<File>();
	ArrayList<File> files = new ArrayList<File>();
	String root = null;

	public FolderPath(String path) {
		this.root = path;
		init(root);

	}

	public ArrayList<File> getFolders() {
		return folders;
	}

	public void setFolders(ArrayList<File> folders) {
		this.folders = folders;
	}

	public ArrayList<File> getFiles() {
		return files;
	}

	public void setFiles(ArrayList<File> files) {
		this.files = files;
	}

	public void init(String root) {
		File f = new File(root);
		File[] fileList = f.listFiles();
		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isDirectory()) {
				folders.add(fileList[i]);
				init(fileList[i].getPath());
			} else {
				files.add(fileList[i]);
			}
		}
	}
}
