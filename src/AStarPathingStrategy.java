import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class AStarPathingStrategy implements PathingStrategy {

    @Override
    public List<Point> computePath(
            Point start,
            Point end,
            Predicate<Point> canPassThrough,
            BiPredicate<Point, Point> withinReach,
            Function<Point, Stream<Point>> potentialNeighbors
    ) {
        Map<Point, Point> cameFrom = new HashMap<>();
        Map<Point, Integer> gScore = new HashMap<>();
        Map<Point, Integer> fScore = new HashMap<>();
        PriorityQueue<Point> openList = new PriorityQueue<>(Comparator.comparingInt(fScore::get));
        Set<Point> closedSet = new HashSet<>();

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, end));
        openList.add(start);

        while (!openList.isEmpty()) {
            Point current = openList.poll();

            if (withinReach.test(current, end)) {
                return reconstructPath(cameFrom, current);
            }

            closedSet.add(current);

            for (Point neighbor : potentialNeighbors.apply(current).collect(Collectors.toList())) {
                if (!canPassThrough.test(neighbor) || closedSet.contains(neighbor)) continue;

                int tentativeG = gScore.get(current) + 1;

                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristic(neighbor, end));
                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        return List.of();
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (cameFrom.containsKey(current)) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}


