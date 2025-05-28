import processing.core.PImage;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class Movable extends ActiveAnimatedEntity {

    private PathingStrategy pathingStrategy;

    public Movable(String id, Point position, List<PImage> images, double actionPeriod, double animationPeriod) {
        super(id, position, images, actionPeriod, animationPeriod);
        // Default to single step pathing strategy
        this.pathingStrategy = new SingleStepPathingStrategy();
    }

    public Point nextPosition(WorldModel world, Point destPos) {
        // Create the predicates and functions needed for the pathing strategy
        Predicate<Point> canPassThrough = (point) ->
                world.withinBounds(point) && !this.isInvalidMove(world, point);

        BiPredicate<Point, Point> withinReach = (start, end) ->
                start.adjacent(end);

        // Compute the path using the pathing strategy
        List<Point> path = pathingStrategy.computePath(
                this.getPosition(),
                destPos,
                canPassThrough,
                withinReach,
                PathingStrategy.CARDINAL_NEIGHBORS
        );

        // If we have a path, return the first step, otherwise stay in place
        if (!path.isEmpty()) {
            return path.get(0);
        } else {
            return this.getPosition();
        }
    }

    public void setPathingStrategy(PathingStrategy strategy) {
        this.pathingStrategy = strategy;
    }

    public abstract boolean moveTo(WorldModel model, Entity target, EventScheduler scheduler);

    /**
     * The entity can move to destination if it's not occupied.
     */
    public boolean isInvalidMove(WorldModel world, Point destination) {
        return world.isOccupied(destination);
    }
}