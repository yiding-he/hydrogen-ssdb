package com.hyd.ssdb.file;

import com.hyd.ssdb.SsdbClient;

import java.util.List;

/**
 * (description)
 * created at 16/04/12
 *
 * @author yidin
 */
public class ListKeys {

    public static void main(String[] args) {
        SsdbClient client = new SsdbClient("localhost", 8888);
        List<String> keys = client.keys("J:", "", 100);
        for (String key : keys) {
            System.out.println(key);
        }
        client.close();
    }
}
