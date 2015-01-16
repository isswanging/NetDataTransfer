package net.util;

import java.io.File;
import java.util.ArrayList;

public class BuildFolder {
    ArrayList<String> folders = new ArrayList<String>();
    ArrayList<String> files = new ArrayList<String>();
    String name;

    public BuildFolder(String root, String content) {
        // 解析目录结构
        analyse(root, content);
        // 构造目录
        structure();

    }

    private void structure() {
        for (String path : folders) {
            File f = new File(path);
            f.mkdir();
        }
    }

    private void analyse(String root, String content) {
        // 处理content，去掉无用的前缀
        String[] r1 = content.split("\\|");
        String[] r2 = r1[0].split("\\\\");
        String regx = "";
        for (int i = 0; i < r2.length - 1; i++) {
            regx = regx + r2[i] + "/";
        }
        String text = content.replaceAll("\\\\", "/").replaceAll(regx, "");

        // 拼接文件夹地址
        String[] folderPaths = text.split("\\|");
        this.setName(folderPaths[0]);

        // 判断是否包含文件
        if (folderPaths[folderPaths.length - 1].contains("*")) {
            // 建立文件夹路径
            for (int i = 0; i < folderPaths.length - 1; i++) {
                setFolders(root, folderPaths[i]);
            }

            // 建立文件路径
            String[] filePaths = folderPaths[folderPaths.length - 1]
                    .split("\\*");
            for (String filePath : filePaths) {
                setFiles(root, filePath);
            }
        } else {
            // 建立文件夹路径
            for (String folderPath : folderPaths) {
                setFolders(root, folderPath);
            }
        }

    }

    public void setFolders(String root, String path) {
        String folderPath = root + "\\" + path;
        folders.add(folderPath);
    }

    public void setFiles(String root, String path) {
        String filePath = root + "\\" + path;
        files.add(filePath);
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setName(String name) {
        this.name = name;
    }
}
