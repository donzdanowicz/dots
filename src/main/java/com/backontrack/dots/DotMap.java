package com.backontrack.dots;

import java.util.*;

import static com.backontrack.dots.Constants.GRID_SIZE_X;
import static com.backontrack.dots.Constants.GRID_SIZE_Y;

public class DotMap extends HashMap<Dot, Set<Dot>> {
    private HashMap<Dot, Set<Dot>> map = new HashMap<>();

    public void createDotMap() {
        for (int row = 0; row < GRID_SIZE_Y; row++) {
            for (int col = 0; col < GRID_SIZE_X; col++) {
                map.put(new Dot(row, col, 0), new HashSet<>());
            }
        }
    }

    public Dot getDot(int row, int col) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().getRow() == row && entry.getKey().getCol() == col)
                .map(Entry::getKey)
                .findFirst()
                .orElse(getDefaultDot());
    }

    public Dot getDefaultDot() {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().getRow() == 19 && entry.getKey().getCol() == 19)
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Dot findFirstAvailableDot() {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().getPlayer() == 0)
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void attributeDotToComputer(Dot dot) {
        dot.setPlayer(2);
    }

    public void attributeDotToPlayer(Dot dot) {
        dot.setPlayer(1);
    }

    public void attributeDotToComputerAndAddConnection(Dot dot1, Dot dot2) {
        map.get(dot1).add(dot2);
        map.get(dot2).add(dot1);
        getDot(dot1.getRow(), dot1.getCol()).setPlayer(2);
        getDot(dot2.getRow(), dot2.getCol()).setPlayer(2);
        map.put(dot1, map.get(dot1));
    }

    public void addConnection(Dot dot1, Dot dot2) {
        Set<Dot> set1 = new HashSet<>();
        Set<Dot> set2 = new HashSet<>();

        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (Objects.equals(entry.getKey().getRow(), dot1.getRow()) && Objects.equals(entry.getKey().getCol(), dot1.getCol())) {
                dot1 = entry.getKey();
                set1 = entry.getValue();
                System.out.println(entry);
            } else if (Objects.equals(entry.getKey().getRow(), dot2.getRow()) && Objects.equals(entry.getKey().getCol(), dot2.getCol())) {
                dot2 = entry.getKey();
                set2 = entry.getValue();
                System.out.println(entry);
            }
        }

        set1.add(dot2);
        set2.add(dot1);

        System.out.printf("ADD CONNECTION: dot1: %d, %d, and dot2: %d, %d", dot1.getRow(), dot1.getCol(), dot2.getRow(), dot2.getCol());
        System.out.println(isAnyConnectionPresent());
        System.out.println("END OF CONNECTION");
        System.out.println("Added connections in DotMap");
    }

    public boolean isAnyConnectionPresent() {
        boolean isAnyConnectionPresent = false;
        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                isAnyConnectionPresent = true;
                System.out.println(entry);
                break;
            }
        }
        return isAnyConnectionPresent;
    }

    public boolean isAnyDotAttributedByComputer() {
        boolean isAnyDotAttributedByComputer = false;
        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (entry.getKey().getPlayer() == 2) {
                isAnyDotAttributedByComputer = true;
                break;
            }
        }
        return isAnyDotAttributedByComputer;
    }

    public Dot getAnyPlayersDotAlone() {
        Dot dot = null;
        boolean isAnyPlayerDotAlone = false;

        while (!isAnyPlayerDotAlone) {
            for (Entry<Dot, Set<Dot>> entry : map.entrySet()) {
                if (entry.getKey().getPlayer() == 1) {
                    int row = entry.getKey().getRow();
                    int col = entry.getKey().getCol();

                    List<Dot> temporaryAloneDotList = new ArrayList<>();

                    temporaryAloneDotList.add(getDot(row + 1, col));
                    temporaryAloneDotList.add(getDot(row - 1, col));
                    temporaryAloneDotList.add(getDot(row, col + 1));
                    temporaryAloneDotList.add(getDot(row, col - 1));
//                    temporaryAloneDotList.add(getDot(row + 1, col + 1));
//                    temporaryAloneDotList.add(getDot(row + 1, col - 1));
//                    temporaryAloneDotList.add(getDot(row - 1, col + 1));
//                    temporaryAloneDotList.add(getDot(row - 1, col - 1));

                    for (Dot temporaryDot : temporaryAloneDotList) {
                        if (temporaryDot.getPlayer() == 1) {
                            break;
                        } else if (temporaryDot.getPlayer() == 2) {
                            isAnyPlayerDotAlone = true;
                        } else {
                            dot = temporaryDot;
                            isAnyPlayerDotAlone = true;
                        }
                    }
                }
            }
        }

        return dot;
    }

    public HashMap<Dot, Set<Dot>> getMapOfComputerAttributedDots() {
        HashMap<Dot, Set<Dot>> computerAttributedDots = new HashMap<>();
        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (entry.getKey().getPlayer() == 2) {
                computerAttributedDots.put(entry.getKey(), entry.getValue());
            }
        }
        return computerAttributedDots;
    }

    public HashMap<Dot, Set<Dot>> getMapOfPlayerAttributedDots() {
        HashMap<Dot, Set<Dot>> playerAttributedDots = new HashMap<>();
        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (entry.getKey().getPlayer() == 1) {
                playerAttributedDots.put(entry.getKey(), entry.getValue());
            }
        }
        return playerAttributedDots;
    }

    public HashMap<Dot, Set<Dot>> getMapOfComputerAttributedDotsWithAtLeastOneConnection() {
        HashMap<Dot, Set<Dot>> computerAttributedDotsWithAtLeastOneConnection = new HashMap<>();
        for (Entry<Dot, Set<Dot>> entry: map.entrySet()) {
            if (entry.getKey().getPlayer() == 2 && !entry.getValue().isEmpty()) {
                computerAttributedDotsWithAtLeastOneConnection.put(entry.getKey(), entry.getValue());
            }
        }
        return computerAttributedDotsWithAtLeastOneConnection;
    }

    public boolean isDotConnectedToAnother(Dot dot) {
        return !map.get(dot).isEmpty();
    }

    @Override
    public String toString() {
        return "DotMap{" +
                "map=" + map +
                '}';
    }
}
