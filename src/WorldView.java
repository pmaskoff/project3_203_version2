import processing.core.PApplet;
import processing.core.PImage;

import java.util.Optional;

public final class WorldView {
    public PApplet screen;
    public WorldModel world;
    private final int tileWidth;
    private final int tileHeight;
    private final Viewport viewport;

    public WorldView(int numRows, int numCols, PApplet screen, WorldModel world, int tileWidth, int tileHeight) {
        this.screen = screen;
        this.world = world;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.viewport = new Viewport(numRows, numCols);
    }

    public Viewport getViewport() {
        return this.viewport;
    }

    public void drawViewport() {
        drawBackground();
        drawEntities();
    }

    public void drawEntities() {
        for (Entity entity : this.world.getEntities()) {
            Point pos = entity.getPosition();

            if (this.viewport.contains(pos)) {
                Point viewPoint = this.viewport.worldToViewport(pos.x, pos.y);
                
                // calculate drawing position
                int drawX = viewPoint.x * this.tileWidth;
                int drawY = viewPoint.y * this.tileHeight;
                
                // center large sprites
                PImage sprite = entity.getCurrentImage();
                if (entity instanceof Thanos) {
                    // center thanos sprite
                    drawX = drawX - (sprite.width - this.tileWidth) / 2;
                    drawY = drawY - (sprite.height - this.tileHeight) / 2;
                }
                
                this.screen.image(sprite, drawX, drawY);
            }
        }
    }

    public void drawBackground() {
        for (int row = 0; row < this.viewport.getNumRows(); row++) {
            for (int col = 0; col < this.viewport.getNumCols(); col++) {
                Point worldPoint = this.viewport.viewportToWorld(col, row);
                Optional<PImage> image = this.world.getBackgroundImage(worldPoint);
                if (image.isPresent()) {
                    this.screen.image(image.get(), col * this.tileWidth, row * this.tileHeight);
                }
            }
        }
    }

    public void shiftView(int colDelta, int rowDelta) {
        int newCol = Functions.clamp(this.viewport.getCol() + colDelta, 0, this.world.getNumCols() - this.viewport.getNumCols());
        int newRow = Functions.clamp(this.viewport.getRow() + rowDelta, 0, this.world.getNumRows() - this.viewport.getNumRows());

        this.viewport.shift(newCol, newRow);
    }
}
