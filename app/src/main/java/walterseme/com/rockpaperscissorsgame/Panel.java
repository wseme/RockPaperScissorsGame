package walterseme.com.rockpaperscissorsgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Custom SurfaceView to handle everything from physics to bitmap.
 *
 * Created by walter
 */
public class Panel extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = Panel.class.getSimpleName();
    private static final String SCISSORS = "scissors";
    private static final String PAPER = "paper";
    private static final String ROCK = "rock";
    private static final String EXPLOSION = "explosion";

    private RockPaperScissorsThread gameLoopThread;
    private ArrayList<Graphic> graphics = new ArrayList<>();
    private ArrayList<Graphic> explosions = new ArrayList<>();
    private Graphic currentGraphic;
    private Graphic.Coordinates lastCoords;
    private SoundPool soundPool;
    private int playbackExplosionSound = 0;
    //used for images
    private Map<Integer, Bitmap> bitmapCache = new HashMap<>();

    /**
     * Constructor called on instantiation.
     * @param context Context of calling activity.
     */
    public Panel(Context context) {
        super(context);
        fillBitmapCache();
        //deprecated
        soundPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 100);
        playbackExplosionSound = soundPool.load(getContext(), R.raw.explosion, 0);
        getHolder().addCallback(this);
        gameLoopThread = new RockPaperScissorsThread(this);
        setFocusable(true);
    }

    private void fillBitmapCache() {
        bitmapCache.put(R.drawable.icon, BitmapFactory.decodeResource(getResources(), R.drawable.icon));
        bitmapCache.put(R.drawable.scissors, BitmapFactory.decodeResource(getResources(), R.drawable.scissors));
        bitmapCache.put(R.drawable.paper, BitmapFactory.decodeResource(getResources(), R.drawable.paper));
        bitmapCache.put(R.drawable.rock, BitmapFactory.decodeResource(getResources(), R.drawable.rock));
        bitmapCache.put(R.drawable.smaller, BitmapFactory.decodeResource(getResources(), R.drawable.smaller));
        bitmapCache.put(R.drawable.small, BitmapFactory.decodeResource(getResources(), R.drawable.small));
        bitmapCache.put(R.drawable.big, BitmapFactory.decodeResource(getResources(), R.drawable.big));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (getHolder()) {
            Graphic graphic = null;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Random random = new Random();
                int rand = Math.abs(random.nextInt() % 3);
                switch (rand) {
                    case 0: graphic = new Graphic(bitmapCache.get(R.drawable.scissors));
                        graphic.setType(SCISSORS);
                        break;
                    case 1: graphic = new Graphic(bitmapCache.get(R.drawable.rock));
                        graphic.setType(ROCK);
                        break;
                    case 2: graphic = new Graphic(bitmapCache.get(R.drawable.paper));
                        graphic.setType(PAPER);
                        break;
                    default:
                        throw new RuntimeException("RANDOM not between 0 and 2: " + rand);
                }
                graphic.getCoordinates().setTouchedX((int) event.getX());
                graphic.getCoordinates().setTouchedY((int) event.getY());
                lastCoords = new Graphic(bitmapCache.get(R.drawable.icon)).getCoordinates();
                lastCoords.setTouchedX(graphic.getCoordinates().getTouchedX());
                lastCoords.setTouchedY(graphic.getCoordinates().getTouchedY());
                currentGraphic = graphic;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                currentGraphic.getCoordinates().setTouchedX((int) event.getX());
                currentGraphic.getCoordinates().setTouchedY((int) event.getY());
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // calculating speed
                calculatingSpeedX((int) event.getX());
                calculatingSpeedY((int) event.getY());
                graphics.add(currentGraphic);
                currentGraphic = null;
            }
            return true;
        }
    }

    /**
     * Speed of the item.
     * Using the difference between start point and release point with smoothing factor.
     *
     * @param currentX X coordinate which represent the last point.
     */
    private void calculatingSpeedX(int currentX) {
        if (currentX != lastCoords.getTouchedX()) {
            int diff = currentX - lastCoords.getTouchedX();
            int amplitude = diff / 10;
            currentGraphic.getSpeed().setX(amplitude);
        } else {
            currentGraphic.getSpeed().setX(0);
        }
    }

    /**
     * Speed of the item.
     * Using the difference between start point and release point with smoothing factor.
     *
     * @param currentY Y coordinate which represent the last point.
     */
    private void calculatingSpeedY(int currentY) {
        if (currentY != lastCoords.getTouchedY()) {
            int diff = currentY - lastCoords.getTouchedY();
            int amplitude = diff / 10;
            currentGraphic.getSpeed().setY(amplitude);
        } else {
            currentGraphic.getSpeed().setY(0);
        }
    }

    /**
     * Update the physics of each item already added to the panel.
     * Not including items which are currently exploding and moved by a touch event.
     */
    public void updatePhysics() {
        Graphic.Coordinates coord;
        Graphic.Speed speed;
        for (Graphic graphic : graphics) {
            coord = graphic.getCoordinates();
            speed = graphic.getSpeed();

            // Direction
            coord.setX(coord.getX() + speed.getX());
            coord.setY(coord.getY() + speed.getY());

            // borders for x
            if (coord.getX() < 0) {
                speed.setX(-speed.getX());
                coord.setX(-coord.getX());
            } else if (coord.getX() + graphic.getBitmap().getWidth() > getWidth()) {
                speed.setX(-speed.getX());
                coord.setX(coord.getX() + getWidth() - (coord.getX() + graphic.getBitmap().getWidth()));
            }

            // borders for y
            if (coord.getY() < 0) {
                speed.setY(-speed.getY());
                coord.setY(-coord.getY());
            } else if (coord.getY() + graphic.getBitmap().getHeight() > getHeight()) {
                speed.setY(-speed.getY());
                coord.setY(coord.getY() + getHeight() - (coord.getY() + graphic.getBitmap().getHeight()));
            }
        }
    }

    /**
     * Check all items on the panel for collisions and find the winner.
     * The loser will added to the list of explosions.
     */
    public void checkForWinners() {
        ArrayList<Graphic> toExplosion = new ArrayList<Graphic>();
        for (Graphic grapics : graphics) {
            for (Graphic battleGraphic : graphics) {
                if (battleGraphic != grapics && !(toExplosion.contains(battleGraphic) || toExplosion.contains(grapics))) {
                    if (!battleGraphic.getType().equals(grapics.getType()) && checkCollision(battleGraphic, grapics)) {
                        if (firstWins(battleGraphic.getType(), grapics.getType())) {
                            toExplosion.add(grapics);
                            soundPool.play(playbackExplosionSound, 1, 1, 0, 0, 1);
                        }
                    }
                } else {
                    continue;
                }
            }
        }
        if (!toExplosion.isEmpty()) {
            explosions.addAll(toExplosion);
            graphics.removeAll(toExplosion);
        }
    }

    /**
     * Mathematical calculation of a collision between two items.
     *
     * @param first First item.
     * @param second Second item.
     * @return Returns true if first and second item hit each other.
     */
    private boolean checkCollision(Graphic first, Graphic second) {
        boolean retValue = false;
        int width = first.getBitmap().getWidth();
        int height = first.getBitmap().getHeight();
        int firstXRangeStart = first.getCoordinates().getX();
        int firstXRangeEnd = firstXRangeStart + width;
        int firstYRangeStart = first.getCoordinates().getY();
        int firstYRangeEnd = firstYRangeStart + height;
        int secondXRangeStart = second.getCoordinates().getX();
        int secondXRangeEnd = secondXRangeStart + width;
        int secondYRangeStart = second.getCoordinates().getY();
        int secondYRangeEnd = secondYRangeStart + height;
        if ((secondXRangeStart >= firstXRangeStart && secondXRangeStart <= firstXRangeEnd)
                || (secondXRangeEnd >= firstXRangeStart && secondXRangeEnd <= firstXRangeEnd)) {
            if ((secondYRangeStart >= firstYRangeStart && secondYRangeStart <= firstYRangeEnd)
                    || (secondYRangeEnd >= firstYRangeStart && secondYRangeEnd <= firstYRangeEnd)) {
                retValue = true;
            }
        }
        return retValue;
    }

    /**
     * True if first type wins, false if second type wins.
     *
     * @param firstType Type of the first object.
     * @param secondType Type of the second object.
     * @return Returns who wins.
     */
    private boolean firstWins(String firstType, String secondType) {
        if (firstType.equals(EXPLOSION) || secondType.equals(EXPLOSION)) {
            return false;
        } else if (firstType.equals(SCISSORS) && secondType.equals(PAPER)) {
            return true;
        } else if (firstType.equals(SCISSORS) && secondType.equals(ROCK)) {
            return false;
        } else if (firstType.equals(PAPER) && secondType.equals(SCISSORS)) {
            return false;
        } else if (firstType.equals(PAPER) && secondType.equals(ROCK)) {
            return true;
        } else if (firstType.equals(ROCK) && secondType.equals(PAPER)) {
            return false;
        } else if (firstType.equals(ROCK) && secondType.equals(SCISSORS)) {
            return true;
        } else {
            throw new RuntimeException("Fight not possible!");
        }
    }

    /**
     * Draw on the SurfaceView.
     * Order:
     * <ul>
     *  <li>Background image</li>
     *  <li>Items on the panel</li>
     *  <li>Explosions</li>
     *  <li>Item moved by hand</li>
     * </ul>
     */
    @Override
    public void onDraw(Canvas canvas) {
        if(canvas == null)
            return;
        // background
        canvas.drawColor(Color.BLUE);
        Bitmap bitmap;
        Graphic.Coordinates coords;
        // draw the normal items
        for (Graphic graphic : graphics) {
            bitmap = graphic.getBitmap();
            coords = graphic.getCoordinates();
            canvas.drawBitmap(bitmap, coords.getX(), coords.getY(), null);
        }

        // draw the explosions
        ArrayList<Graphic> finishedExplosion = new ArrayList<>();
        for (Graphic graphic : explosions) {
            if (!graphic.getType().equals(EXPLOSION)) {
                graphic.setType(EXPLOSION);
                graphic.setExplosionStep(0);
                graphic.getSpeed().setX(0);
                graphic.getSpeed().setY(0);
                graphic.setBitmap(bitmapCache.get(R.drawable.smaller));
                bitmap = graphic.getBitmap();
                coords = graphic.getCoordinates();
                canvas.drawBitmap(bitmap, coords.getX(), coords.getY(), null);
            } else {
                switch (graphic.getExplosionStep()) {
                    case 10: bitmap = bitmapCache.get(R.drawable.small);
                        graphic.setBitmap(bitmap);
                        break;
                    case 20: bitmap = bitmapCache.get(R.drawable.big);
                        graphic.setBitmap(bitmap);
                        break;
                    case 30: bitmap = bitmapCache.get(R.drawable.small);
                        graphic.setBitmap(bitmap);
                        break;
                    case 40: bitmap = bitmapCache.get(R.drawable.smaller);
                        graphic.setBitmap(bitmap);
                        break;
                    default: bitmap = graphic.getBitmap();
                }
                coords = graphic.getCoordinates();
                canvas.drawBitmap(bitmap, coords.getX(), coords.getY(), null);
                graphic.setExplosionStep(graphic.getExplosionStep() + 1);
            }
            if (graphic.getExplosionStep() > Graphic.EXPLOSION_MAX_STEPS) {
                finishedExplosion.add(graphic);
            }
        }

        // remove all objects that are already fully exploded
        if (!finishedExplosion.isEmpty()) {
            explosions.removeAll(finishedExplosion);
        }

        // draw current graphic last
        if (currentGraphic != null) {
            bitmap = currentGraphic.getBitmap();
            coords = currentGraphic.getCoordinates();
            canvas.drawBitmap(bitmap, coords.getX(), coords.getY(), null);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!gameLoopThread.isAlive()) {
            gameLoopThread = new RockPaperScissorsThread(this);
        }
        gameLoopThread.setRunning(true);
        gameLoopThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        gameLoopThread.setRunning(false);
        while (retry) {
            try {
                gameLoopThread.join();
                retry = false;
            } catch (InterruptedException e) {
                //try it again and again
            }
        }
        Log.i(TAG, "Thread terminated");
    }
}