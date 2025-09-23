package com.backontrack.dots;

import java.util.Objects;

public class Dot {
    private Integer row;
    private Integer col;
    private Integer player;
    private boolean blocked = false;

    public Dot(Integer row, Integer col, Integer player) {
        this.row = row;
        this.col = col;
        this.player = player;
    }

    public Integer getRow() {
        return row;
    }

    public Integer getCol() {
        return col;
    }

    public Integer getPlayer() {
        return player;
    }

    public void setPlayer(Integer player) {
        this.player = player;
    }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    @Override
    public String toString() {
        return "Dot{" +
                "row=" + row +
                ", col=" + col +
                ", player=" + player +
                ", blocked=" + blocked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dot dot = (Dot) o;
        return Objects.equals(row, dot.row) && Objects.equals(col, dot.col);
                //&& Objects.equals(player, dot.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col
                //, player
                );
    }
}
