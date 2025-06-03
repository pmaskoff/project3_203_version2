import processing.core.PImage;

import java.util.List;

/**
 * infected dude variant
 */
public class DudeInfected extends Dude {

    public DudeInfected(String id, Point position, List<PImage> images, double actionPeriod,
                       double animationPeriod, int resourceLimit) {
        super(id, position, images, actionPeriod, animationPeriod, resourceLimit);
    }

    @Override
    public void executeActivity(WorldModel world, ImageStore imageStore, EventScheduler scheduler) {
        // infect nearby dudes
        infectNearbyDudes(world, scheduler, imageStore);
        moveRandomly(world, scheduler);
        
        // schedule next activity
        scheduler.scheduleEvent(this, new Activity(this, world, imageStore), this.getActionPeriod());
    }

    @Override
    public boolean moveTo(WorldModel world, Entity target, EventScheduler scheduler) {
        // move to space
        Point nextPos = this.nextPosition(world, target.getPosition());
        if (!this.getPosition().equals(nextPos)) {
            world.moveEntity(scheduler, this, nextPos);
        }
        return false;
    }

    @Override
    public boolean transform(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        // no further transform
        return false;
    }
    
    private void infectNearbyDudes(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        // infect if standing
        if (isOnInfectedTile(world)) {
            List<Entity> nearbyEntities = world.getEntities().stream()
                .filter(entity -> (entity instanceof DudeNotFull || entity instanceof DudeFull) && 
                        entity.getPosition().distanceSquared(this.getPosition()) <= 2) // within 1 tile
                .toList();
                
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Dude dude && !(dude instanceof DudeInfected)) {
                    // transform into infected
                    DudeInfected infectedDude = new DudeInfected(
                        dude.getId() + "_infected",
                        dude.getPosition(),
                        imageStore.getImageList("alien"), // alien appearance
                        dude.getActionPeriod() / 2.0, // 2x speed
                        dude.getAnimationPeriod() / 2.0,
                        dude.getResourceLimit()
                    );
                    
                    world.removeEntity(scheduler, dude);
                    scheduler.unscheduleAllEvents(dude);
                    world.addEntity(infectedDude);
                    infectedDude.scheduleActions(scheduler, world, imageStore);
                }
            }
        }
    }
    
    private boolean isOnInfectedTile(WorldModel world) {
        if (world.withinBounds(this.getPosition())) {
            return world.getBackgroundCell(this.getPosition()).getId().equals("infected");
        }
        return false;
    }
    
    private void moveRandomly(WorldModel world, EventScheduler scheduler) {
        // random movement
        java.util.Random random = new java.util.Random();
        int[] dx = {-1, 0, 1, 0};
        int[] dy = {0, -1, 0, 1};
        int direction = random.nextInt(4);
        
        Point newPos = new Point(
            this.getPosition().x + dx[direction],
            this.getPosition().y + dy[direction]
        );
        
        // move if valid
        if (world.withinBounds(newPos) && !world.isOccupied(newPos)) {
            world.moveEntity(scheduler, this, newPos);
        }
    }
} 