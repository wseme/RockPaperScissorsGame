package walterseme.com.rockpaperscissorsgame;

import android.graphics.Canvas;

/**
 * Thread class to perform the so called "game loop".
 *
 * Created by walter
 */
class RockPaperScissorsThread extends Thread {
    private Panel panel;
    private boolean isRunning = false;

    public RockPaperScissorsThread(Panel panel) {
        this.panel = panel;
    }

    public void setRunning(boolean run) {
        isRunning = run;
    }

    /**
     * Game loop.
     * Order of performing:
     * 1. update physics
     * 2. check for winners
     * 3. draw everything
     */
    @Override
    public void run() {
        Canvas c;
        while (isRunning) {
            c = null;
            try {
                c = panel.getHolder().lockCanvas(null);
                synchronized (panel.getHolder()) {
                    panel.updatePhysics();
                    panel.checkForWinners();
                    panel.onDraw(c);
                }
            } finally {
                // do in a finally so that if an exception is thrown
                // don't leave the Surface in an inconsistent state
                if (c != null)
                    panel.getHolder().unlockCanvasAndPost(c);
            }
        }
    }
}