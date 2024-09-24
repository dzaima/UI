package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class Window {
  public final WindowImpl impl;
  public boolean focused = true;
  public Hijack hijack;
  public Devtools tools;
  
  public int w, h;
  public float dpi = 1f;
  public int mx=0, my=0;
  public int dx=0, dy=0;
  public int frameCount;
  
  public Window(WindowInit init) {
    this.impl = Windows.makeWindowImpl(this, init);
  }
  
  ///////// interface \\\\\\\\\
  
  public abstract void setup();
  public abstract void resized(); // called on startup
  public abstract boolean draw(Graphics g, boolean full); // return if drew anything different from previous frame
  public abstract void eventTick();
  public abstract void maybeResize();
  public abstract void tick();
  public /*open*/ void stopped() { }
  public /*open*/ void focused() { focused = true; }
  public /*open*/ void unfocused() { focused = false; }
  public /*open*/ void hints() { }
  public /*open*/ boolean requiresDraw() { return true; } // just a hint
  protected /*open*/ void onFrameError(Throwable e) {
    createTools();
    if (tools!=null) tools.errorReport(e);
    else closeOnNext();
  }
  
  public Click[] btns = new Click[]{new Click(this, Click.LEFT), new Click(this, Click.RIGHT), new Click(this, Click.CENTER), new Click(this, Click.BACK), new Click(this, Click.FORWARD)};
  public abstract void mouseDown(Click c);
  public abstract void mouseUp(int x, int y, Click c);
  public abstract void scroll(float dx, float dy, boolean shift);
  public abstract boolean key(Key key, int scancode, KeyAction action);
  public abstract void typed(int p);
  
  ///////// interaction \\\\\\\\\
  
  public XY windowPos() { return impl.windowPos(); }
  
  public void setTitle(String s) { impl.setTitle(s); }
  public String getTitle() { return impl.getTitle(); }
  
  public /*open*/ int framerate() { return 60; } // target frames per second
  public /*open*/ int tickDelta() { return 1000/60; } // milliseconds to wait between ticks; shouldn't be less than framerate (aka, tps≤fps)
  
  
  public enum CursorType {
    REGULAR, HAND, IBEAM, CROSSHAIR,
    N_RESIZE, E_RESIZE, S_RESIZE, W_RESIZE,
    NE_RESIZE, NW_RESIZE, SE_RESIZE, SW_RESIZE,
    EW_RESIZE, NS_RESIZE,
    NESW_RESIZE, NWSE_RESIZE,
  }
  public void setCursor(CursorType c) { impl.setCursor(c); }
  
  public void openFolder(Path initialDir, Consumer<Path> onResult) { impl.openFolder(initialDir, onResult); }
  public void openFile(String filter, Path initialDir, Consumer<Path> onResult) { impl.openFile(filter, initialDir, onResult); }
  public void saveFile(String filter, Path initialDir, String initialName, Consumer<Path> onResult) { impl.saveFile(filter, initialDir, initialName, onResult); }
  
  public void copyString(String s) { impl.copyString(s); }
  public void pasteString(Consumer<String> f) { impl.pasteString(f); }
  
  public final void closeOnNext() { impl.closeOnNext(); } // close the window on next tick (or possibly later if the window was just opened)
  public void closeRequested() { closeOnNext(); } // called when user wants to close window
  
  public Devtools createTools() { return impl.createTools(); }
  public void enqueue(Runnable r) { impl.enqueue(r); } // can be called from any thread; runs r during next tick
  public void runOnUIThread(Runnable r) { impl.runOnUIThread(r); } // can be called from any thread; runs r at an arbitrary point on the UI thread (i.e. always can be replaced with enqueue, but not the other way around)
  
  public enum WindowType {
    NORMAL, POPUP
  }
  public void setType(WindowType t) {
    impl.setType(t);
  }
  
  protected boolean setupDone = false;
  public final AtomicBoolean updateSize = new AtomicBoolean(true);
  public int nodrawFrames=-60, framesSinceSetup;
  public enum DrawReq { NONE, TEMP, PARTIAL, FULL };
  public DrawReq nextTick() { // 0-don't draw; 1-needs partial draw; 2-needs full draw
    long sns = System.nanoTime();
    Devtools t = tools;
    if (t!=null) t.timeStart(sns);
    
    
    // important events
    if (!setupDone) {
      try {
        setup();
      } catch (Throwable e) {
        Log.error("ui", "Errored during setup:");
        Log.stacktrace("ui", e);
        setupDone = true;
        closeOnNext();
        throw new RuntimeException(e);
      }
      setupDone = true;
    } else if (framesSinceSetup<10) framesSinceSetup++;
    boolean resize = updateSize.getAndSet(false);
    if (resize) {
      impl.runResize();
      resized();
    }
    
    // regular events
    impl.runEvents();
    eventTick();
    if (t!=null) t.time("event");
    
    // process nodes
    maybeResize();
    tick();
    if (t!=null) t.time("tick");
    maybeResize();
    
    int toolsRedraw = hijack!=null? hijack.hRedraw() : 0;
    boolean draw = resize || toolsRedraw==2 || requiresDraw();
    boolean full = resize || toolsRedraw!=0 || impl.requiresDraw();
    return full? DrawReq.FULL : draw? DrawReq.PARTIAL : (impl.misuseBuffers && nodrawFrames<20? DrawReq.TEMP : DrawReq.NONE);
  }
  public void nextDraw(Graphics g, boolean full) {
    Devtools t = tools;
    impl.draw(() -> {
      int prevCount = g.canvas.getSaveCount();
      boolean drew = draw(g, full);
      if (g.canvas.getSaveCount() != prevCount) throw new RuntimeException("Unmatched saves and restores");
      
      if (drew) nodrawFrames = Math.min(nodrawFrames, 0);
      else nodrawFrames++;
      if (t!=null) t.time("draw");
    });
  }
  public void postDraw(boolean didDraw, long sns) {
    frameCount++;
    dx = 0; dy = 0;
    Devtools t = tools;
    if (t!=null) {
      if (!didDraw) t.timeDirect("draw", 0);
      t.time("flush");
      t.time("all", sns);
    }
  }
  
  
  
  public static void onFrameError(Window w, Throwable t) {
    Log.error("ui", "Error during frame:");
    onError(w, t, "ui", null);
  }
  public static void onError(Window w, Throwable t, String type, Node n) {
    try {
      Log.stacktrace(type, t);
      Tools.sleep(1000/60);
      if (w!=null) w.onFrameError(t);
      if (n!=null) Devtools.debugMe(n);
    } catch (Throwable t2) {
      Log.error(type, "Error during onError:");
      Log.stacktrace(type, t2);
    }
  }
  
  // deprecated methods for erroring on usage
  @Deprecated public final void resized(Surface s) { throw new AssertionError(); }
}
