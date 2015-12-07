package com.hyd.ssdb.protocol;

import com.hyd.ssdb.util.IdScore;
import com.hyd.ssdb.util.KeyValue;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * (description)
 * created at 15-11-30
 *
 * @author Yiding
 */
public class Response {

    private String header;

    private List<String> blocks = new ArrayList<String>();

    public Response(byte[] data) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(data));
        boolean lengthFlag = true;  // 当前行是否是长度，如果是 false 则表示是内容
        boolean headerExists = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (line.length() == 0) {
                break;
            }

            if (!lengthFlag) {
                if (!headerExists) {
                    this.header = line;
                    headerExists = true;
                } else {
                    this.blocks.add(line);
                }
            }

            lengthFlag = !lengthFlag;
        }
    }

    public String getHeader() {
        return header;
    }

    public List<String> getBlocks() {
        return blocks;
    }

    public String joinBlocks(char joint) {
        StringBuilder sb = new StringBuilder();

        for (String block : blocks) {
            sb.append(block).append(joint);
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    public String firstBlock() {
        return blocks.isEmpty() ? null : blocks.get(0);
    }

    /**
     * 获得 int 类型的返回值
     *
     * @return 如果回应的内容是没有找到，则返回 -1，否则返回回应的内容
     */
    public int getIntResult() {
        if (header.equals("not_found")) {
            return -1;
        } else {
            return Integer.parseInt(firstBlock());
        }
    }

    public long getLongResult() {
        return Long.parseLong(firstBlock());
    }

    public List<KeyValue> getKeyValues() {
        List<KeyValue> keyValues = new ArrayList<KeyValue>();

        for (int i = 0; i + 1 < blocks.size(); i += 2) {
            String key = blocks.get(i);
            String value = blocks.get(i + 1);
            keyValues.add(new KeyValue(key, value));
        }

        return keyValues;
    }

    public List<IdScore> getIdScores() {
        List<IdScore> idScores = new ArrayList<IdScore>();

        for (int i = 0; i + 1 < blocks.size(); i += 2) {
            String key = blocks.get(i);
            String value = blocks.get(i + 1);
            idScores.add(new IdScore(key, Integer.parseInt(value)));
        }

        return idScores;
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
