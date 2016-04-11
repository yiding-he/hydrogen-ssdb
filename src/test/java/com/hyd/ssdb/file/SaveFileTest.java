package com.hyd.ssdb.file;

import com.hyd.ssdb.SsdbClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * (description)
 * created at 16/04/12
 *
 * @author yidin
 */
public class SaveFileTest {

    private static SsdbClient client;

    public static void main(String[] args) throws Exception {
        client = new SsdbClient("localhost", 8888);
        saveFiles();
        client.close();
    }

    private static void saveFiles() throws IOException {
        String root = "J:/CardImages";
        iterateDir(new File(root));
    }

    private static void iterateDir(File dir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isFile()) {
                saveFileToSsdb(child);
            } else if (child.isDirectory()) {
                iterateDir(child);
            }
        }
    }

    private static void saveFileToSsdb(File file) throws IOException {
        byte[] content = new byte[(int) file.length()];
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            IOUtils.read(fis, content);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        client.set(file.getAbsolutePath(), content);
        System.out.println("File saved: " + file.getAbsolutePath());
    }
}
