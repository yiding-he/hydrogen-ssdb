package com.hyd.ssdb.protocol;

import com.hyd.ssdb.util.IdScore;
import com.hyd.ssdb.util.KeyValue;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;


/**
 * (description)
 * created at 16/06/15
 *
 * @author yiding_he
 */
public class Response {

    private Block head;

    private List<Block> body = new ArrayList<>();

    private final Charset charset;

    public Response(Charset charset) {
        this.charset = charset;
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

    public byte[] firstBlockBytes() {
        return this.body.isEmpty() ? null : this.body.get(0).getData();
    }

    public String joinBlocks(char joint) {
        return joinBlocks(new String(new char[]{joint}));
    }

    public String joinBlocks(String joint) {

        if (this.body.isEmpty()) {
            return "";

        } else if (this.body.size() == 1) {
            return this.body.get(0).toString();

        } else {
            return this.body.stream().map(Block::toString).collect(Collectors.joining(joint));
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
        return this.body.stream()
            .map(block -> block.toString(charset))
            .collect(Collectors.toList());
    }

    public List<byte[]> getByteBlocks() {
        return this.body.stream().map(Block::getData).collect(Collectors.toList());
    }

    public List<KeyValue> getKeyValues() {
        List<KeyValue> keyValues = new ArrayList<>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            keyValues.add(new KeyValue(
                body.get(i).getData(), body.get(i + 1).getData(), charset
            ));
        }

        return keyValues;
    }

    public List<IdScore> getIdScores() {
        List<IdScore> idScores = new ArrayList<>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            String key = body.get(i).toString();
            String value = body.get(i + 1).toString();
            idScores.add(new IdScore(key, Long.parseLong(value)));
        }

        return idScores;
    }

    public List<String> getIds() {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i + 1 < body.size(); i += 2) {
            String key = body.get(i).toString();
            ids.add(key);
        }

        return ids;
    }

    public Map<byte[], byte[]> getBlocksAsMap() {

        Map<byte[], byte[]> map = new HashMap<>();
        List<KeyValue> keyValues = getKeyValues();

        for (KeyValue keyValue : keyValues) {
            map.put(keyValue.getKey(), keyValue.getValue());
        }

        return map;
    }

    public Map<String, String> getBlocksAsStringMap(Charset charset) {

        Map<String, String> map = new HashMap<>();
        List<KeyValue> keyValues = getKeyValues();

        for (KeyValue keyValue : keyValues) {
            map.put(
                new String(keyValue.getKey(), charset).intern(),
                new String(keyValue.getValue(), charset).intern()
            );
        }

        return map;
    }
}
