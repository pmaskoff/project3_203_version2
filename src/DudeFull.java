import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DudeFull extends Dude {

    public DudeFull(String id, Point position, List<PImage> images, double actionPeriod,
                double animationPeriod, int resourceLimit) {
        super(id, position, images, actionPeriod, animationPeriod, resourceLimit);
    }
    public boolean moveTo(WorldModel world, Entity target, EventScheduler scheduler) {
        if (this.getPosition().adjacent(target.getPosition())) {
            return true;
        } else {
            return super.moveTo(world, target, scheduler);
        }
    }

    public void executeActivity(WorldModel world, ImageStore imageStore, EventScheduler scheduler) {
        // check infected tile
        if (isOnInfectedTile(world)) {
            transformIntoAlien(world, scheduler, imageStore);
            return;
        }
        
        Optional<Entity> fullTarget = world.findNearest(this.getPosition(), new ArrayList<>(List.of(House.class)));

        if (fullTarget.isPresent() && this.moveTo(world, fullTarget.get(), scheduler)) {
            this.transform(world, scheduler, imageStore);
        } else {
            scheduler.scheduleEvent(this, new Activity(this, world, imageStore), this.getActionPeriod());
        }
    }

    private boolean isOnInfectedTile(WorldModel world) {
        if (world.withinBounds(this.getPosition())) {
            return world.getBackgroundCell(this.getPosition()).getId().equals("infected");
        }
        return false;
    }
    
    private void transformIntoAlien(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        DudeInfected infectedDude = new DudeInfected(
            this.getId() + "_infected",
            this.getPosition(),
            imageStore.getImageList("alien"), // Use alien images for infected appearance
            this.getActionPeriod() / 2.0, // 2x speed
            this.getAnimationPeriod() / 2.0,
            this.getResourceLimit()
        );
        
        world.removeEntity(scheduler, this);
        scheduler.unscheduleAllEvents(this);
        world.addEntity(infectedDude);
        infectedDude.scheduleActions(scheduler, world, imageStore);
    }

    public boolean transform(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        Movable dude = new DudeNotFull(this.getId(), this.getPosition(), this.getImages(),
                this.getActionPeriod(), this.getAnimationPeriod(), this.getResourceLimit(), 0);
        world.removeEntity(scheduler, this);

        world.addEntity(dude);
        dude.scheduleActions(scheduler, world, imageStore);

        return true;
    }
}
