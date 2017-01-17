package com.hyd.ssdb.protocol;

import com.hyd.ssdb.util.IdScore;
import com.hyd.ssdb.util.KeyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (description)
 * created at 16/06/15
 *
 * @author yiding_he
 */
public class Response {

    private Block head;

    private List<Block> body = new ArrayList<Block>();

    public Response() {
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

    public String firstBlock() {
        return this.body.isEmpty() ? null : this.body.get(0).toString();
    }

    public String joinBlocks(char joint) {

        if (this.body.isEmpty()) {
            return "";

        } else if (this.body.size() == 1) {
            return this.body.get(0).toString();

        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.body.size() - 1; i++) {
                sb.append(this.body.get(i)).append(joint);
            }
            sb.append(this.body.get(this.body.size() - 1));
            return sb.toString();
        }
    }

    // 以字节的方式返回数据
    public byte[] getBytes() {
        return this.body.isEmpty() ? new byte[0] : this.body.get(0).getData();
    }

    public Integer getIntResult() {
        if (this.head.toString().equals("not_found")) {
            return null;
        }
        return this.body.isEmpty() ? 0 : Integer.parseInt(this.body.get(0).toString());
    }

    public int getIntResult(int defaultValue) {
        Integer result = getIntResult();
        return result == null ? defaultValue : result;
    }

    public Long getLongResult() {
        if (this.head.toString().equals("not_found")) {
            return null;
        }
        return this.body.isEmpty() ? 0 : Long.parseLong(this.body.get(0).toString());
    }

    public long getLongResult(long defaultValue) {
        Long result = getLongResult();
        return result == null ? defaultValue : result;
    }

    public List<String> getBlocks() {
        ArrayList<String> blocks = new ArrayList<String>();
        for (Block block : body) {
            blocks.add(block.toString());
        }
        return blocks;
    }

    public List<KeyValue> getKeyValues() {
        List<KeyValue> keyValues = new ArrayList<KeyValue>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            String key = body.get(i).toString();
            String value = body.get(i + 1).toString();
            keyValues.add(new KeyValue(key, value));
        }

        return keyValues;
    }

    public List<IdScore> getIdScores() {
        List<IdScore> idScores = new ArrayList<IdScore>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            String key = body.get(i).toString();
            String value = body.get(i + 1).toString();
            idScores.add(new IdScore(key, Long.parseLong(value)));
        }

        return idScores;
    }

    public List<String> getIds() {
        List<String> ids = new ArrayList<String>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            String key = body.get(i).toString();
            ids.add(key);
        }

        return ids;
    }

    public Map<String, String> getBlocksAsMap() {

        Map<String, String> map = new HashMap<String, String>();
        List<KeyValue> keyValues = getKeyValues();

        for (KeyValue keyValue : keyValues) {
            map.put(keyValue.getKey(), keyValue.getValue());
        }

        return map;
    }
}
