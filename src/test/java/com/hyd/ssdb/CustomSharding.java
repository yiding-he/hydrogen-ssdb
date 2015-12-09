package com.hyd.ssdb;

/**
 * (description)
 * created at 15-12-9
 *
 * @author Yiding
 */
public class CustomSharding {

    public static void main(String[] args) {
        SsdbClient client = new SsdbClient(new AjiaSharding());

        client.set("name", "ajia");
        System.out.println(client.get("name"));

        client.close();
    }

    //////////////////////////////////////////////////////////////

}
