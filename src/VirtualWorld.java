import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import processing.core.*;

public final class VirtualWorld extends PApplet {
    private static String[] ARGS;

    public static final int VIEW_WIDTH = 640;
    public static final int VIEW_HEIGHT = 480;
    public static final int TILE_WIDTH = 32;
    public static final int TILE_HEIGHT = 32;

    public static final int VIEW_COLS = VIEW_WIDTH / TILE_WIDTH;
    public static final int VIEW_ROWS = VIEW_HEIGHT / TILE_HEIGHT;

    public static final String IMAGE_LIST_FILE_NAME = "imagelist";
    public static final String DEFAULT_IMAGE_NAME = "background_default";
    public static final int DEFAULT_IMAGE_COLOR = 0x808080;

    public static final String FAST_FLAG = "-fast";
    public static final String FASTER_FLAG = "-faster";
    public static final String FASTEST_FLAG = "-fastest";
    public static final double FAST_SCALE = 0.5;
    public static final double FASTER_SCALE = 0.25;
    public static final double FASTEST_SCALE = 0.10;

    public String loadFile = "world.sav";
    public long startTimeMillis = 0;
    public double timeScale = 1.0;

    private  ImageStore imageStore;
    private  WorldModel world;
    private  WorldView view;
    private  EventScheduler scheduler;

    public void settings() {
        size(VIEW_WIDTH, VIEW_HEIGHT);
    }

    /*
       Processing entry point for "sketch" setup.
    */
    public void setup() {
        parseCommandLine(ARGS);
        loadImages(IMAGE_LIST_FILE_NAME);
        loadWorld(loadFile, this.imageStore);

        this.view = new WorldView(VIEW_ROWS, VIEW_COLS, this, world, TILE_WIDTH, TILE_HEIGHT);
        this.scheduler = new EventScheduler();
        this.startTimeMillis = System.currentTimeMillis();
        this.scheduleActions(world, scheduler, imageStore);
    }

    public void draw() {
        double appTime = (System.currentTimeMillis() - startTimeMillis) * 0.001;
        double frameTime = (appTime - scheduler.getCurrentTime())/timeScale;
        this.update(frameTime);
        view.drawViewport();
    }

    public void update(double frameTime){
        scheduler.updateOnTime(frameTime);
    }

    // debugging and p5
    public void mousePressed() {
        Point pressed = mouseToPoint();
        System.out.println("CLICK! " + pressed.x + ", " + pressed.y);

        // disco fever event
        createDiscoFeverEvent(pressed);

        Optional<Entity> entityOptional = world.getOccupant(pressed);
        if (entityOptional.isPresent()) {
            Entity entity = entityOptional.get();
            System.out.println(entity.getId() + ": " + entity.getClass());
        }
    }
    
    private void createDiscoFeverEvent(Point clickPos) {
        // create infected area
        Background infectedBackground = new Background("infected", imageStore.getImageList("infected"));
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Point infectedPos = new Point(clickPos.x + dx, clickPos.y + dy);
                if (world.withinBounds(infectedPos)) {
                    world.setBackgroundCell(infectedPos, infectedBackground);
                }
            }
        }
        
        // transform nearest dude
        transformNearestDude(clickPos);
        
        // spawn thanos
        if (!world.isOccupied(clickPos)) {
            Thanos thanos = new Thanos(
                "thanos_" + System.currentTimeMillis(),
                clickPos,
                imageStore.getImageList("thanos"),
                1.0, // action period
                0.5  // animation period
            );
            
            world.addEntity(thanos);
            thanos.scheduleActions(scheduler, world, imageStore);
            
            System.out.println("DISCO FEVER! Thanos has arrived at " + clickPos.x + ", " + clickPos.y);
        }
        
        // transform standing dudes
        transformDudesOnInfectedTiles();
    }
    
    private void transformNearestDude(Point clickPos) {
        // find nearest dude
        Optional<Entity> nearestDude = world.getEntities().stream()
            .filter(entity -> (entity instanceof DudeNotFull || entity instanceof DudeFull) && 
                            !(entity instanceof DudeInfected))
            .min((e1, e2) -> Integer.compare(
                e1.getPosition().distanceSquared(clickPos),
                e2.getPosition().distanceSquared(clickPos)
            ));
            
        if (nearestDude.isPresent() && nearestDude.get() instanceof Dude dude) {
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
            
            System.out.println("Nearest dude " + dude.getId() + " has been infected!");
        }
    }
    
    private void transformDudesOnInfectedTiles() {
        // find standing dudes
        List<Entity> dudesToTransform = new ArrayList<>();
        
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Dude) {
                Point pos = entity.getPosition();
                if (world.withinBounds(pos) && 
                    world.getBackgroundCell(pos).getId().equals("infected")) {
                    dudesToTransform.add(entity);
                }
            }
        }
        
        // transform each dude
        for (Entity dude : dudesToTransform) {
            if (dude instanceof Dude dudeEntity && !(dudeEntity instanceof DudeInfected)) {
                DudeInfected infectedDude = new DudeInfected(
                    dudeEntity.getId() + "_infected",
                    dudeEntity.getPosition(),
                    imageStore.getImageList("alien"), // alien appearance
                    dudeEntity.getActionPeriod() / 2.0, // 2x speed
                    dudeEntity.getAnimationPeriod() / 2.0,
                    dudeEntity.getResourceLimit()
                );
                
                world.removeEntity(scheduler, dudeEntity);
                scheduler.unscheduleAllEvents(dudeEntity);
                world.addEntity(infectedDude);
                infectedDude.scheduleActions(scheduler, world, imageStore);
            }
        }
    }

    public void scheduleActions(WorldModel world, EventScheduler scheduler, ImageStore imageStore) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Animatable) {
                ((Animatable) entity).scheduleActions(scheduler, world, imageStore);
            } else if (entity instanceof Active) {
                ((Active) entity).scheduleActions(scheduler, world, imageStore);
            }
        }
    }

    private Point mouseToPoint() {
        return view.getViewport().viewportToWorld(mouseX / TILE_WIDTH, mouseY / TILE_HEIGHT);
    }

    public void keyPressed() {
        if (key == CODED) {
            int dx = 0;
            int dy = 0;

            switch (keyCode) {
                case UP -> dy -= 1;
                case DOWN -> dy += 1;
                case LEFT -> dx -= 1;
                case RIGHT -> dx += 1;
            }
            view.shiftView(dx, dy);
        }
    }

    public static Background createDefaultBackground(ImageStore imageStore) {
        return new Background(DEFAULT_IMAGE_NAME, imageStore.getImageList(DEFAULT_IMAGE_NAME));
    }

    public static PImage createImageColored(int width, int height, int color) {
        PImage img = new PImage(width, height, RGB);
        img.loadPixels();
        Arrays.fill(img.pixels, color);
        img.updatePixels();
        return img;
    }

    public void loadImages(String filename) {
        this.imageStore = new ImageStore(createImageColored(TILE_WIDTH, TILE_HEIGHT, DEFAULT_IMAGE_COLOR));
        try {
            Scanner in = new Scanner(new File(filename));
            WorldModel.loadImages(in, imageStore,this);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    public void loadWorld(String file, ImageStore imageStore) {
        this.world = new WorldModel();
        try {
            Scanner in = new Scanner(new File(file));
            world.load(in, imageStore, createDefaultBackground(imageStore));
        } catch (FileNotFoundException e) {
            Scanner in = new Scanner(file);
            world.load(in, imageStore, createDefaultBackground(imageStore));
        }
    }

    public void parseCommandLine(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case FAST_FLAG -> timeScale = Math.min(FAST_SCALE, timeScale);
                case FASTER_FLAG -> timeScale = Math.min(FASTER_SCALE, timeScale);
                case FASTEST_FLAG -> timeScale = Math.min(FASTEST_SCALE, timeScale);
                default -> loadFile = arg;
            }
        }
    }

    public static void main(String[] args) {
        VirtualWorld.ARGS = args;
        PApplet.main(VirtualWorld.class);
    }

    public static List<String> headlessMain(String[] args, double lifetime){
        VirtualWorld.ARGS = args;

        VirtualWorld virtualWorld = new VirtualWorld();
        virtualWorld.setup();
        virtualWorld.update(lifetime);

        return virtualWorld.world.log();
    }
}
