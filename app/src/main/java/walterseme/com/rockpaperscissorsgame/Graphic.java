package walterseme.com.rockpaperscissorsgame;

import android.graphics.Bitmap;

/**
 * Contains an object to draw on a specific position.
 * Created by walter
 */
public class Graphic {

    private Bitmap bitmap;
    private Coordinates coordinates;
    private Speed speed;

    //Object type which could be rock, scissors, paper or explosion.
    //Could switch to Enum
    private String type;

    // explosion animation steps, total 50 steps
    private int explosionStep = 0;
    public static final int EXPLOSION_MAX_STEPS = 50;

    public Graphic(Bitmap bitmap) {
        this.bitmap = bitmap;
        coordinates = new Coordinates();
        speed = new Speed();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
    public Speed getSpeed() {
        return speed;
    }
    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }

    public void setExplosionStep(int step) {
        if(step >=0 && step <= EXPLOSION_MAX_STEPS)
            explosionStep = step;
    }
    public int getExplosionStep() {
        return explosionStep;
    }

    public class Speed {
        // speed in  directions, negative values are backward
        private int x = 1;
        private int y = 1;

        public void setX(int speed) {
            x = speed;
        }
        public int getX() {
            return x;
        }

        public void setY(int speed) {
            y = speed;
        }
        public int getY() {
            return y;
        }

        public String toString() {
            return "Speed: x: " + x + " | y: " + y;
        }
    }

    public class Coordinates {
        private int x = 0;
        private int y = 0;

        //upper left corner x
        public void setX(int value) {
            x = value;
        }
        public int getX() {
            return x;
        }

        //upper left corner y
        public void setY(int value) {y = value;}
        public int getY() {return y;}

        //center of bitmap x, for touch events
        public void setTouchedX(int value) {
            x = value - bitmap.getWidth() / 2;
        }
        public int getTouchedX() {
            return x + bitmap.getWidth() / 2;
        }

        //center of bitmap y, for touch events
        public void setTouchedY(int value) {
            y = value - bitmap.getHeight() / 2;
        }
        public int getTouchedY() {
            return y + bitmap.getHeight() / 2;
        }

        public String toString() {
            return "Coordinates: (" + x + "/" + y + ")";
        }
    }

}

