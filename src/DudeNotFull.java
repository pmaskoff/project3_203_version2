import processing.core.PImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DudeNotFull extends Dude {

    private int resourceCount;

    public DudeNotFull(String id, Point position, List<PImage> images, double actionPeriod,
                double animationPeriod, int resourceLimit, int resourceCount) {
        super(id, position, images, actionPeriod, animationPeriod, resourceLimit);
        this.resourceCount = resourceCount;
    }

    public void executeActivity(WorldModel world, ImageStore imageStore, EventScheduler scheduler) {
        // check infected tile
        if (isOnInfectedTile(world)) {
            transformIntoAlien(world, scheduler, imageStore);
            return;
        }
        
        Optional<Entity> target = world.findNearest(this.getPosition(), new ArrayList<>(Arrays.asList(Tree.class, Sapling.class)));

        if (target.isEmpty() || !this.moveTo(world, target.get(), scheduler) || !this.transform(world, scheduler, imageStore)) {
            scheduler.scheduleEvent(this, new Activity(this, world, imageStore), this.getActionPeriod());
        }
    }

    public boolean moveTo(WorldModel world, Entity target, EventScheduler scheduler) {
        if (this.getPosition().adjacent(target.getPosition())) {
            this.resourceCount += 1;
            ((Plant) target).decreaseHealth();
            return true;
        } else {
            return super.moveTo(world, target, scheduler);
        }
    }

    public boolean transform(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        if (this.resourceCount >= this.getResourceLimit()) {
            Movable dude = new DudeFull(this.getId(), this.getPosition(), this.getImages(), this.getActionPeriod(),
                    this.getAnimationPeriod(), this.getResourceLimit());

            world.removeEntity(scheduler, this);
            scheduler.unscheduleAllEvents(this);

            world.addEntity(dude);
            dude.scheduleActions(scheduler, world, imageStore);

            return true;
        }

        return false;
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

}
