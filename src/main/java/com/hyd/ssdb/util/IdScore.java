package com.hyd.ssdb.util;

/**
 * id - score 对
 * created at 15-12-7
 *
 * @author Yiding
 */
public class IdScore {

    private String id;

    private int score;

    public IdScore() {
    }

    public IdScore(String id, int score) {
        this.id = id;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
