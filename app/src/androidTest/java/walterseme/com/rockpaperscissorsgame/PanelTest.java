package walterseme.com.rockpaperscissorsgame;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by walter
 */
@RunWith(AndroidJUnit4.class)
public class PanelTest {

    Panel panel;
    
    @Before
    public void setUp() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        panel = new Panel(appContext);
    }


}