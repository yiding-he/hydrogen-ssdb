package com.hyd.ssdb.protocol;

import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.util.Bytes;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求
 * created at 15-11-30
 *
 * @author Yiding
 */
public class Request {

    private Block header;

    private final List<Block> blocks = new ArrayList<Block>();

    public Request(String command) {
        String[] tokens = command.split("\\s+");
        readTokens(tokens);
    }

    public Request(Object... tokens) {
        if (tokens.length == 0) {
            throw new SsdbException("command is empty");
        }

        readTokens(tokens);
    }

    private void readTokens(Object[] tokens) {

        // info、dsize等命令只有 command，其他命令至少有 command 和 key 两个部分，然后可能有后面其他参数
        if (tokens.length < 1) {
            throw new SsdbException("Command empty.");
        }

        this.header = new Block(tokens[0].toString());

        for (int i = 1; i < tokens.length; i++) {
            Object token = tokens[i];
            Block block;
            if (token instanceof byte[]) {
                block = new Block((byte[]) token);
            } else {
                block = new Block(token.toString());
            }
            this.blocks.add(block);
        }
    }

    public Block getHeader() {
        return header;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public String getKey() {
		// info、dsize等命令 blocks.size() == 0
		if (blocks.size() == 0) {
			return "";
		}
        return blocks.get(0).toString();    // 第一个参数一定是 key，用来决定其放在哪台服务器上
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(header.toString()).append(' ');

        for (Block block : blocks) {
            sb.append(block.toString()).append(' ');
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public byte[] toBytes() {
        byte[][] byteArrays = new byte[blocks.size() + 2][];

        byteArrays[0] = header.toBytes();
        for (int i = 0; i < blocks.size(); i++) {
            byteArrays[i + 1] = blocks.get(i).toBytes();
        }
        byteArrays[byteArrays.length - 1] = new byte[]{'\n'};

        return Bytes.concat(byteArrays);
    }
}
