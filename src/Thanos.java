import processing.core.PImage;

import java.util.List;
import java.util.Random;

/**
 * thanos spreads infection
 */
public class Thanos extends ActiveAnimatedEntity {
    public static final String THANOS_KEY = "thanos";
    public static final int THANOS_NUM_PROPERTIES = 2;
    public static final int THANOS_ACTION_PERIOD = 0;
    public static final int THANOS_ANIMATION_PERIOD = 1;
    
    private static final Random random = new Random();

    public Thanos(String id, Point position, List<PImage> images, double actionPeriod, double animationPeriod) {
        super(id, position, images, actionPeriod, animationPeriod);
    }

    @Override
    public void executeActivity(WorldModel world, ImageStore imageStore, EventScheduler scheduler) {
        // infect current tile
        infectTile(world, this.getPosition(), imageStore);
        
        // transform nearby dudes
        transformNearbyDudes(world, scheduler, imageStore);
        
        // move randomly
        moveRandomly(world, scheduler);
        
        // schedule next activity
        scheduler.scheduleEvent(this, new Activity(this, world, imageStore), this.getActionPeriod());
    }
    
    private void infectTile(WorldModel world, Point position, ImageStore imageStore) {
        if (world.withinBounds(position)) {
            Background infectedBackground = new Background("infected", imageStore.getImageList("infected"));
            world.setBackgroundCell(position, infectedBackground);
        }
    }
    
    private void transformNearbyDudes(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        // find nearby dudes
        List<Entity> nearbyEntities = world.getEntities().stream()
            .filter(entity -> (entity instanceof DudeNotFull || entity instanceof DudeFull) && 
                    entity.getPosition().distanceSquared(this.getPosition()) <= 9) // within 3 tiles
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
    
    private void moveRandomly(WorldModel world, EventScheduler scheduler) {
        // random direction
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