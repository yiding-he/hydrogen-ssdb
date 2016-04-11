package com.hyd.ssdb.file;

import com.hyd.ssdb.SsdbClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * (description)
 * created at 16/04/12
 *
 * @author yidin
 */
public class ReadFile {

    public static void main(String[] args) throws IOException {
        SsdbClient client = new SsdbClient("localhost", 8888);
        String key = "J:\\CardImages\\1427938a-ac39-43b5-925f-99b1e5c02257\\shuxue\\0\\002902010336\\01A.jpg";
        byte[] bytes = client.getBytes(key);

        File outputFile = new File("target/output.jpg");
        FileUtils.writeByteArrayToFile(outputFile, bytes);

        System.out.println("File saved.");
        client.close();
    }
}
