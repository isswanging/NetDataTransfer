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

    public ArrayList<File> getFiles() {
        return files;
    }

    public void init(String root) {
        File f = new File(root);
        File[] fileList = f.listFiles();
        for (File file : fileList) {
            if (file.isDirectory()) {
                folders.add(file);
                init(file.getPath());
            } else {
                files.add(file);
            }
        }
    }
}
