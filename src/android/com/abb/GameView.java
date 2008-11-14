package android.com.abb;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import java.lang.System;
import java.lang.Thread;

import android.com.abb.Game;


public class GameView extends SurfaceView implements SurfaceHolder.Callback {
  class GameThread extends Thread {
    public GameThread(SurfaceHolder surface_holder) {
      game_ = null;
      running_ = true;
      surface_holder_ = surface_holder;
    }

    @Override
    public void run() {
      // Wait util the game instance has been set.
      while (game_ == null) {
        try {
          Thread.sleep(100l, 0);
        } catch (InterruptedException ex) {}
        continue;
      }

      synchronized (game_) {
        game_.Reset();
      }

      // Since our target platform is a mobile device, we should do what we can
      // to save power. In the case of a game like this, we should 1) limit the
      // framerate to something "reasonable" and 2) pause the updates as much as
      // possible. Here we define the maximum framerate which needs to make the
      // trade off between graphics fluidity and power savings.
      final float kMaxFrameRate = 15.0f;  // Frames / second.
      final float kMinTimeStep = 1.0f / kMaxFrameRate;  // Seconds.

      // The timers available through the Java APIs appear sketchy in general. I
      // was able to find the following resource useful:
      // http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks
      long time = System.nanoTime();

      while (running_) {
        // Calculate the interval between this and the previous frame. See note
        // above regarding system timers. If we have exceeded our framerate
        // budget, sleep. TODO: Look into the cost of these clocks.
        long current_time = System.nanoTime();
        float time_step = (float)(current_time - time) * 1.0e-9f;
        time = current_time;
        if (time_step < kMinTimeStep) {
          float remaining_time = kMinTimeStep - time_step;
          time_step = kMinTimeStep;
          try {
            long sleep_milliseconds = (long)(remaining_time * 1.0e3f);
            remaining_time -= sleep_milliseconds * 1.0e-3f;
            int sleep_nanoseconds = (int)(remaining_time * 1.0e9f);
            Thread.sleep(sleep_milliseconds, sleep_nanoseconds);
          } catch (InterruptedException ex) {
            // If someone notified the thread, just forget about it and continue
            // on. It's not worth the cycles to handle.
          }
        }

        Canvas canvas = null;
        try {
          canvas = surface_holder_.lockCanvas(null);
          synchronized (game_) {
            game_.Update(time_step, canvas);
          }
        } finally {
          if (canvas != null) {
            surface_holder_.unlockCanvasAndPost(canvas);
          }
        }
      }
    }

    public void SetGame(Game game) {
      game_ = game;
    }

    public void Pause(boolean pause) {
      // TODO: Implement this.
    }

    public void Halt() {
      running_ = false;
    }

    boolean running_;
    Game game_;
    SurfaceHolder surface_holder_;
  }

  private Context context_;
  private Game game_;
  private GameThread game_thread_;

  public GameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    context_ = context;

    // Make sure we get key events and register our interest in hearing about
    // changes to our surface.
    setFocusable(true);
    SurfaceHolder surface_holder = getHolder();
    surface_holder.addCallback(this);

    // Create thread only; it's started in surfaceCreated()
    game_thread_ = new GameThread(surface_holder);
  }

  public void SetGame(Game game) {
    game.LoadResources(context_.getResources());
    game_ = game;
    game_thread_.SetGame(game);
  }

  /** Standard override to get key-press events. */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent msg) {
    synchronized (game_) {
      return game_.OnKeyDown(keyCode);
    }
  }

  /** Standard override for key-up. We actually care about these, so we can turn
   * off the engine or stop rotating. */
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent msg) {
    synchronized (game_) {
      return game_.OnKeyUp(keyCode);
    }
  }

  /** Standard window-focus override. Notice focus lost so we can pause on focus
   * lost. e.g. user switches to take a call. */
  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    game_thread_.Pause(hasWindowFocus);
  }

  /** Callback invoked when the Surface has been created and is ready to be
   * used. */
  public void surfaceCreated(SurfaceHolder holder) {
    // Start the thread here so that we don't busy-wait in run() waiting for the
    // surface to be created
    game_thread_.start();
  }

  /** Callback invoked when the surface dimensions change. */
  public void surfaceChanged(SurfaceHolder holder, int format,
                             int width, int height) {}  // Don't care.

  /** Callback invoked when the Surface has been destroyed and must no longer be
   * touched. WARNING: after this method returns, the Surface/Canvas must never
   * be touched again! */
  public void surfaceDestroyed(SurfaceHolder holder) {
    // we have to tell thread to shut down & wait for it to finish, or else it
    // might touch the Surface after we return and explode
    boolean retry = true;
    game_thread_.Halt();
    while (retry) {
      try {
        game_thread_.join();
        retry = false;
      } catch (InterruptedException e) {}
    }
  }
}