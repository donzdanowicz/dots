package com.backontrack.dots;

import java.util.*;

/**
 * PolygonDetector - Java 11 compatible.
 *
 * Uses:
 *  - dotMap: Map<Dot, Set<Dot>> where each Set contains CONNECTED adjacent dots (8-neighbors if connected).
 *  - Dot must have getRow(), getCol(), getPlayer(), isBlocked(), setBlocked(...)
 *  - Dot.equals/hashCode MUST depend only on row & col.
 *
 * Rules implemented:
 *  - updateBlockedDots(dotMap) finds polygons already DRAWN by owners and marks dots
 *    INSIDE opponent polygons as blocked (forbidden to form new connections).
 *  - findFirstPolygonForComputer(dotMap) finds the first possible polygon the computer
 *    can make (using candidate edges between adjacent non-blocked computer dots, excluding
 *    candidate edges that would cross existing drawn edges). Polygon must have size >= 4
 *    and enclose at least one player dot.
 *  - playerHasPossiblePolygon(dotMap) similar but for player -> returns boolean.
 */
public class PolygonDetector {

    // ---------------- Public API ----------------

    /** Update dot.blocked flags: any dot that lies inside any polygon drawn by the opponent becomes blocked. */
    public void updateBlockedDots(Map<Dot, Set<Dot>> dotMap) {
        // 1) clear blocking
        for (Dot d : dotMap.keySet()) d.setBlocked(false);

        // 2) find drawn polygons for players (owner 1 and 2) using ONLY already drawn edges
        Map<Dot, Set<Dot>> drawnAdjPlayer = buildDrawnAdjacency(dotMap, 1);
        Map<Dot, Set<Dot>> drawnAdjComputer = buildDrawnAdjacency(dotMap, 2);

        List<List<Dot>> playerPolys = findAllCyclesAsLists(drawnAdjPlayer);
        List<List<Dot>> computerPolys = findAllCyclesAsLists(drawnAdjComputer);

        // 3) For each player polygon, block enclosed computer dots; for each computer polygon, block enclosed player dots
        for (List<Dot> poly : playerPolys) {
            for (Dot d : dotMap.keySet()) {
                if (d.getPlayer() == 2 && isInsidePolygon(poly, d)) d.setBlocked(true);
            }
        }
        for (List<Dot> poly : computerPolys) {
            for (Dot d : dotMap.keySet()) {
                if (d.getPlayer() == 1 && isInsidePolygon(poly, d)) d.setBlocked(true);
            }
        }
    }

    /** Find first polygon the computer can form (candidate edges considered). Returned as Map<Dot,Dot> edges. */
    public Map<Dot, Dot> findFirstPolygonForComputer(Map<Dot, Set<Dot>> dotMap) {
        // build candidate graph for computer (owner=2) considering blocked flags and existing drawn edges blocking crossings
        Map<Dot, Set<Dot>> candidate = buildCandidateGraph(dotMap, 2);
        return findFirstCycle(candidate, dotMap, 1); // opponent = player (1)
    }

    /** Find all polygons the computer can form (candidate edges considered). */
    public List<Map<Dot, Dot>> findAllPolygonsForComputer(Map<Dot, Set<Dot>> dotMap) {
        Map<Dot, Set<Dot>> candidate = buildCandidateGraph(dotMap, 2);
        return findAllCycles(candidate, dotMap, 1);
    }

    /** Check whether the human player (1) has any possible polygon (candidate edges) enclosing at least one computer dot (2). */
    public boolean playerHasPossiblePolygon(Map<Dot, Set<Dot>> dotMap) {
        Map<Dot, Set<Dot>> candidate = buildCandidateGraph(dotMap, 1);
        Map<Dot, Dot> poly = findFirstCycle(candidate, dotMap, 2);
        return !poly.isEmpty();
    }

    // ---------------- Build drawn adjacency (only existing drawn edges) ----------------

    /** Build adjacency map from dotMap but only links between same-owner drawn edges. This represents already-drawn graph. */
    private Map<Dot, Set<Dot>> buildDrawnAdjacency(Map<Dot, Set<Dot>> dotMap, int ownerValue) {
        Map<Dot, Set<Dot>> adj = new HashMap<>();
        for (Dot d : dotMap.keySet()) {
            if (d.getPlayer() != ownerValue) continue;
            for (Dot nb : dotMap.getOrDefault(d, Collections.emptySet())) {
                if (nb.getPlayer() != ownerValue) continue;
                // both are same owner and the edge is drawn (entry in dotMap)
                adj.computeIfAbsent(d, k -> new HashSet<>()).add(nb);
            }
        }
        return adj;
    }

    // ---------------- Candidate graph builder (owner-owned adjacency, skipping blocked) ----------------

    /**
     * Build candidate graph for ownerValue:
     * - candidate edge exists between two owner-owned adjacent dots (8-neighbors) if:
     *    a) the edge already exists (drawn) OR
     *    b) the candidate edge would not cross any already-drawn edge (from either owner)
     * - blocked dots are ignored (no candidate edges touching them)
     */
    private Map<Dot, Set<Dot>> buildCandidateGraph(Map<Dot, Set<Dot>> dotMap, int ownerValue) {
        Map<Long, Dot> byRC = new HashMap<>();
        for (Dot d : dotMap.keySet()) byRC.put(rcKey(d.getRow(), d.getCol()), d);

        Set<Edge> existingEdges = collectExistingEdges(dotMap);

        Map<Dot, Set<Dot>> adj = new HashMap<>();
        for (Dot a : dotMap.keySet()) {
            if (a.getPlayer() != ownerValue) continue;
            if (a.isBlocked()) continue; // can't be used

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    Dot b = byRC.get(rcKey(a.getRow() + dr, a.getCol() + dc));
                    if (b == null) continue;
                    if (b.getPlayer() != ownerValue) continue;
                    if (b.isBlocked()) continue;

                    Edge cand = new Edge(a, b);
                    boolean already = existingEdges.contains(cand);
                    if (already || !crossesAny(cand, existingEdges)) {
                        adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                        adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                    }
                }
            }
        }
        return adj;
    }

    // ---------------- Collect existing drawn edges -------------------------------------

    private Set<Edge> collectExistingEdges(Map<Dot, Set<Dot>> dotMap) {
        Set<Edge> out = new HashSet<>();
        for (Map.Entry<Dot, Set<Dot>> e : dotMap.entrySet()) {
            Dot u = e.getKey();
            for (Dot v : e.getValue()) {
                out.add(new Edge(u, v));
            }
        }
        return out;
    }

    // ---------------- Cycle-finding (first) on an adjacency map ------------------------

    private Map<Dot, Dot> findFirstCycle(Map<Dot, Set<Dot>> adj, Map<Dot, Set<Dot>> dotMap, int opponentValue) {
        Set<Dot> visited = new HashSet<>();

        for (Dot start : adj.keySet()) {
            if (visited.contains(start)) continue;

            Deque<Dot> stack = new ArrayDeque<>();
            Set<Dot> onStack = new HashSet<>();
            Map<Dot, Dot> got = dfsFindFirst(adj, dotMap, start, null, visited, stack, onStack, opponentValue);
            if (!got.isEmpty()) return got;
        }
        return Collections.emptyMap();
    }

    private Map<Dot, Dot> dfsFindFirst(Map<Dot, Set<Dot>> adj,
                                       Map<Dot, Set<Dot>> dotMap,
                                       Dot current,
                                       Dot parent,
                                       Set<Dot> visited,
                                       Deque<Dot> stack,
                                       Set<Dot> onStack,
                                       int opponentValue) {
        visited.add(current);
        stack.addLast(current);
        onStack.add(current);

        for (Dot neighbor : adj.getOrDefault(current, Collections.emptySet())) {
            if (neighbor.equals(parent)) continue;

            if (!visited.contains(neighbor)) {
                Map<Dot, Dot> got = dfsFindFirst(adj, dotMap, neighbor, current, visited, stack, onStack, opponentValue);
                if (!got.isEmpty()) return got;
            } else if (onStack.contains(neighbor)) {
                List<Dot> cycle = extractCycleFromStack(stack, neighbor);
                if (cycle.size() >= 4) {
                    if (enclosesOpponentDot(cycle, dotMap, opponentValue)) {
                        return buildEdgeMap(cycle);
                    }
                }
            }
        }

        stack.removeLast();
        onStack.remove(current);
        return Collections.emptyMap();
    }

    // ---------------- Cycle-finding (all) on an adjacency map -------------------------

    private List<Map<Dot, Dot>> findAllCycles(Map<Dot, Set<Dot>> adj, Map<Dot, Set<Dot>> dotMap, int opponentValue) {
        List<Map<Dot, Dot>> out = new ArrayList<>();
        Set<Dot> visited = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (Dot start : adj.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Dot> stack = new ArrayDeque<>();
            Set<Dot> onStack = new HashSet<>();
            dfsFindAll(adj, dotMap, start, null, visited, stack, onStack, opponentValue, out, seen);
        }
        return out;
    }

    private void dfsFindAll(Map<Dot, Set<Dot>> adj,
                            Map<Dot, Set<Dot>> dotMap,
                            Dot current,
                            Dot parent,
                            Set<Dot> visited,
                            Deque<Dot> stack,
                            Set<Dot> onStack,
                            int opponentValue,
                            List<Map<Dot, Dot>> out,
                            Set<String> seen) {
        visited.add(current);
        stack.addLast(current);
        onStack.add(current);

        for (Dot neighbor : adj.getOrDefault(current, Collections.emptySet())) {
            if (neighbor.equals(parent)) continue;

            if (!visited.contains(neighbor)) {
                dfsFindAll(adj, dotMap, neighbor, current, visited, stack, onStack, opponentValue, out, seen);
            } else if (onStack.contains(neighbor)) {
                List<Dot> cycle = extractCycleFromStack(stack, neighbor);
                if (cycle.size() >= 4 && enclosesOpponentDot(cycle, dotMap, opponentValue)) {
                    String key = normalizeCycleKey(cycle);
                    if (!seen.contains(key)) {
                        seen.add(key);
                        out.add(buildEdgeMap(cycle));
                    }
                }
            }
        }

        stack.removeLast();
        onStack.remove(current);
    }

    // ---------------- Find all cycles as lists (used for marking blocked dots) ---------

    /**
     * Finds all cycles in the provided adjacency map (no enclosing check).
     * Returns list of cycles (each cycle as List<Dot> in vertex order).
     */
    private List<List<Dot>> findAllCyclesAsLists(Map<Dot, Set<Dot>> adj) {
        List<List<Dot>> out = new ArrayList<>();
        Set<Dot> visited = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (Dot start : adj.keySet()) {
            if (visited.contains(start)) continue;
            Deque<Dot> stack = new ArrayDeque<>();
            Set<Dot> onStack = new HashSet<>();
            dfsCollectCycles(adj, start, null, visited, stack, onStack, out, seen);
        }
        return out;
    }

    private void dfsCollectCycles(Map<Dot, Set<Dot>> adj,
                                  Dot current,
                                  Dot parent,
                                  Set<Dot> visited,
                                  Deque<Dot> stack,
                                  Set<Dot> onStack,
                                  List<List<Dot>> out,
                                  Set<String> seen) {
        visited.add(current);
        stack.addLast(current);
        onStack.add(current);

        for (Dot neighbor : adj.getOrDefault(current, Collections.emptySet())) {
            if (neighbor.equals(parent)) continue;

            if (!visited.contains(neighbor)) {
                dfsCollectCycles(adj, neighbor, current, visited, stack, onStack, out, seen);
            } else if (onStack.contains(neighbor)) {
                List<Dot> cycle = extractCycleFromStack(stack, neighbor);
                if (cycle.size() >= 4) {
                    String key = normalizeCycleKey(cycle);
                    if (!seen.contains(key)) {
                        seen.add(key);
                        out.add(cycle);
                    }
                }
            }
        }

        stack.removeLast();
        onStack.remove(current);
    }

    // ---------------- Helpers: cycle extraction, build edges -------------------------

    private List<Dot> extractCycleFromStack(Deque<Dot> stack, Dot neighbor) {
        List<Dot> cycle = new ArrayList<>();
        Iterator<Dot> it = stack.descendingIterator(); // current -> ... -> oldest
        while (it.hasNext()) {
            Dot d = it.next();
            cycle.add(d);
            if (d.equals(neighbor)) break;
        }
        Collections.reverse(cycle); // neighbor ... current
        return cycle;
    }

    private Map<Dot, Dot> buildEdgeMap(List<Dot> cycle) {
        Map<Dot, Dot> edges = new LinkedHashMap<>();
        int n = cycle.size();
        for (int i = 0; i < n; i++) {
            Dot a = cycle.get(i);
            Dot b = cycle.get((i + 1) % n);
            edges.put(a, b);
        }
        return edges;
    }

    // ---------------- Geometry: enclosure tests -------------------------------------

    private boolean enclosesOpponentDot(List<Dot> polygon, Map<Dot, Set<Dot>> dotMap, int opponentValue) {
        for (Dot d : dotMap.keySet()) {
            if (d.getPlayer() != opponentValue) continue;
            if (isInsidePolygon(polygon, d)) return true;
        }
        return false;
    }

    /**
     * Ray-casting point-in-polygon test.
     * polygon: List<Dot> vertex order, using getRow() as y and getCol() as x.
     */
    private boolean isInsidePolygon(List<Dot> polygon, Dot point) {
        if (polygon == null || polygon.size() < 3) return false;
        boolean inside = false;
        double px = point.getCol();
        double py = point.getRow();

        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i).getCol();
            double yi = polygon.get(i).getRow();
            double xj = polygon.get(j).getCol();
            double yj = polygon.get(j).getRow();
            boolean intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    // ---------------- Edge crossing checks -----------------------------------------

    private static final class Edge {
        final Dot a, b;
        Edge(Dot u, Dot v) { if (cmp(u, v) <= 0) { a = u; b = v; } else { a = v; b = u; } }
        private static int cmp(Dot d1, Dot d2) {
            int c = Integer.compare(d1.getRow(), d2.getRow());
            return (c != 0) ? c : Integer.compare(d1.getCol(), d2.getCol());
        }
        @Override public boolean equals(Object o) { if (!(o instanceof Edge)) return false; Edge e = (Edge) o; return a.equals(e.a) && b.equals(e.b); }
        @Override public int hashCode() { return 31 * a.hashCode() + b.hashCode(); }
    }

    private boolean crossesAny(Edge cand, Set<Edge> existing) {
        for (Edge e : existing) {
            // sharing endpoints is allowed
            if (cand.a.equals(e.a) || cand.a.equals(e.b) || cand.b.equals(e.a) || cand.b.equals(e.b)) continue;
            if (segmentsIntersect(cand.a, cand.b, e.a, e.b)) return true;
        }
        return false;
    }

    private boolean segmentsIntersect(Dot p1, Dot p2, Dot q1, Dot q2) {
        int o1 = orient(p1, p2, q1);
        int o2 = orient(p1, p2, q2);
        int o3 = orient(q1, q2, p1);
        int o4 = orient(q1, q2, p2);
        if (o1 != o2 && o3 != o4) return true;
        if (o1 == 0 && onSeg(p1, q1, p2)) return true;
        if (o2 == 0 && onSeg(p1, q2, p2)) return true;
        if (o3 == 0 && onSeg(q1, p1, q2)) return true;
        if (o4 == 0 && onSeg(q1, p2, q2)) return true;
        return false;
    }

    private int orient(Dot a, Dot b, Dot c) {
        long x1 = b.getCol() - a.getCol();
        long y1 = b.getRow() - a.getRow();
        long x2 = c.getCol() - a.getCol();
        long y2 = c.getRow() - a.getRow();
        long v = x1 * y2 - y1 * x2;
        return Long.compare(v, 0);
    }

    private boolean onSeg(Dot a, Dot b, Dot c) {
        return Math.min(a.getCol(), c.getCol()) <= b.getCol() && b.getCol() <= Math.max(a.getCol(), c.getCol()) &&
                Math.min(a.getRow(), c.getRow()) <= b.getRow() && b.getRow() <= Math.max(a.getRow(), c.getRow());
    }

    // ---------------- Utils --------------------------------------------------------

    private long rcKey(int r, int c) { return (((long) r) << 32) ^ (c & 0xffffffffL); }

    private String normalizeCycleKey(List<Dot> cyc) {
        int n = cyc.size();
        int best = 0;
        for (int i = 1; i < n; i++) if (lt(cyc.get(i), cyc.get(best))) best = i;
        String f = keyFrom(cyc, best, +1);
        String b = keyFrom(cyc, (best - 1 + n) % n, -1);
        return (f.compareTo(b) <= 0) ? f : b;
    }
    private boolean lt(Dot a, Dot b) {
        if (a.getRow() != b.getRow()) return a.getRow() < b.getRow();
        return a.getCol() < b.getCol();
    }
    private String keyFrom(List<Dot> cyc, int start, int step) {
        StringBuilder sb = new StringBuilder();
        int n = cyc.size();
        int idx = start;
        for (int k = 0; k < n; k++) {
            Dot d = cyc.get(idx);
            sb.append(d.getRow()).append(',').append(d.getCol()).append(';');
            idx = (idx + step + n) % n;
        }
        return sb.toString();
    }
}



//import java.util.*;
//
//import static com.backontrack.dots.Constants.COMPUTER;
//import static com.backontrack.dots.Constants.PLAYER;
//
//public class PolygonDetector {
//
//    /**
//     * Finds the first closed polygon of computer's dots
//     * that encloses at least one player's dot.
//     */
//    public Map<Dot, Dot> findFirstPolygon(Map<Dot, Set<Dot>> dotMap) {
//        Set<Dot> visited = new HashSet<>();
//
//        for (Dot start : dotMap.keySet()) {
//            if (!visited.contains(start) && start.getPlayer() == COMPUTER) {
//                Map<Dot, Dot> polygon = dfsFindCycle(dotMap, start, visited);
//                if (!polygon.isEmpty()) {
//                    if (containsPlayerDot(polygon.keySet(), dotMap)) {
//                        return polygon;
//                    }
//                }
//            }
//        }
//        return Collections.emptyMap();
//    }
//
//    /**
//     * Finds all closed polygons of computer's dots
//     * that enclose at least one player's dot.
//     */
//    public List<Map<Dot, Dot>> findAllPolygons(Map<Dot, Set<Dot>> dotMap) {
//        Set<Dot> visited = new HashSet<>();
//        List<Map<Dot, Dot>> result = new ArrayList<>();
//
//        for (Dot start : dotMap.keySet()) {
//            if (!visited.contains(start) && start.getPlayer() == COMPUTER) {
//                Map<Dot, Dot> polygon = dfsFindCycle(dotMap, start, visited);
//                if (!polygon.isEmpty()) {
//                    if (containsPlayerDot(polygon.keySet(), dotMap)) {
//                        result.add(polygon);
//                    }
//                }
//            }
//        }
//        return result;
//    }
//
//
//    /**
//     * DFS to find cycle of computer dots.
//     */
//    private Map<Dot, Dot> dfsFindCycle(Map<Dot, Set<Dot>> dotMap, Dot start, Set<Dot> visited) {
//        Map<Dot, Dot> pathMap = new LinkedHashMap<>();
//        Stack<Dot> stack = new Stack<>();
//        Map<Dot, Dot> parent = new HashMap<>();
//
//        stack.push(start);
//        parent.put(start, null);
//
//        while (!stack.isEmpty()) {
//            Dot current = stack.pop();
//            visited.add(current);
//
//            for (Dot neighbor : dotMap.getOrDefault(current, Collections.emptySet())) {
//                if (neighbor.getPlayer() != COMPUTER) continue;
//
//                if (!visited.contains(neighbor)) {
//                    parent.put(neighbor, current);
//                    stack.push(neighbor);
//                } else if (parent.get(current) != null && !neighbor.equals(parent.get(current))) {
//                    // Found a cycle → reconstruct path
//                    return buildCyclePath(parent, current, neighbor);
//                }
//            }
//        }
//        return Collections.emptyMap();
//    }
//
//    /**
//     * Build polygon edge map from parent trace.
//     */
//    private Map<Dot, Dot> buildCyclePath(Map<Dot, Dot> parent, Dot a, Dot b) {
//        List<Dot> pathA = new ArrayList<>();
//        List<Dot> pathB = new ArrayList<>();
//
//        Dot cur = a;
//        while (cur != null) {
//            pathA.add(cur);
//            cur = parent.get(cur);
//        }
//
//        cur = b;
//        while (cur != null) {
//            pathB.add(cur);
//            cur = parent.get(cur);
//        }
//
//        // Find LCA
//        int i = pathA.size() - 1;
//        int j = pathB.size() - 1;
//        while (i >= 0 && j >= 0 && pathA.get(i).equals(pathB.get(j))) {
//            i--;
//            j--;
//        }
//
//        Set<Dot> cycle = new LinkedHashSet<>();
//        for (int k = 0; k <= i + 1; k++) cycle.add(pathA.get(k));
//        for (int k = j + 1; k >= 0; k--) cycle.add(pathB.get(k));
//
//        // Build edge map
//        Map<Dot, Dot> polygon = new LinkedHashMap<>();
//        Dot prev = null, first = null;
//        for (Dot d : cycle) {
//            if (first == null) first = d;
//            if (prev != null) polygon.put(prev, d);
//            prev = d;
//        }
//        if (prev != null && first != null) polygon.put(prev, first);
//
//        return polygon;
//    }
//
//    /**
//     * Check if polygon contains at least one player's dot.
//     */
//    private boolean containsPlayerDot(Set<Dot> polygonVertices, Map<Dot, Set<Dot>> dotMap) {
//        for (Dot dot : dotMap.keySet()) {
//            if (dot.getPlayer() == PLAYER && isInsidePolygon(dot, polygonVertices)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * Ray-casting algorithm: check if point lies inside polygon.
//     */
//    private boolean isInsidePolygon(Dot point, Set<Dot> polygon) {
//        List<Dot> vertices = new ArrayList<>(polygon);
//        int n = vertices.size();
//        boolean inside = false;
//
//        for (int i = 0, j = n - 1; i < n; j = i++) {
//            Dot vi = vertices.get(i);
//            Dot vj = vertices.get(j);
//
//            boolean intersect = ((vi.getRow() > point.getRow()) != (vj.getRow() > point.getRow())) &&
//                    (point.getCol() < (vj.getCol() - vi.getCol()) * (point.getRow() - vi.getRow()) / (double)(vj.getRow() - vi.getRow()) + vi.getCol());
//            if (intersect) inside = !inside;
//        }
//
//        return inside;
//    }
//}
//
