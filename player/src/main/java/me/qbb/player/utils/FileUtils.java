package me.qbb.player.utils;

import java.io.File;

/**
 * 创建时间 2017/11/29 15:10
 *
 * @author Qian Bing Bing
 *         类说明 文件工具类
 */

public class FileUtils {

    public static void RecursionDeleteFile(File file) {
        try {
            if (file.isFile()) {
                boolean delete = file.delete();
                return;
            }
            if (file.isDirectory()) {
                File[] childFile = file.listFiles();
                if (childFile == null || childFile.length == 0) {
                    file.delete();
                    return;
                }
                for (File f : childFile) {
                    RecursionDeleteFile(f);
                }
                file.delete();
            }
        } catch (Exception e) {

        }

    }
}
