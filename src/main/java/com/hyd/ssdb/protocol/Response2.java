package com.hyd.ssdb.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * (description)
 * created at 16/06/15
 *
 * @author yiding_he
 */
public class Response2 {

    private Block head;

    private List<Block> body = new ArrayList<Block>();

    public Response2() {
    }

    public Block getHead() {
        return head;
    }

    public void setHead(Block head) {
        this.head = head;
    }

    public List<Block> getBody() {
        return body;
    }

    public void setBody(List<Block> body) {
        this.body = body;
    }

    public void addBodyBlock(Block block) {
        this.body.add(block);
    }

    public Response toResponse() {
        Response response = new Response();
        response.setHeader(head);
        response.setBlocks(body);
        response.setContent(body.get(0).toBytes());
        return response;
    }
}
