package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.gui.io.*;
import dzaima.utils.XY;
import io.github.humbleui.skija.Surface;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class Window {
  public final WindowImpl impl;
  public boolean focused = true;
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
  public abstract void resized(Surface s); // called on startup
  public abstract boolean draw(Graphics g, boolean full); // return if drew anything different from previous frame
  public abstract void eventTick();
  public abstract void maybeResize();
  public abstract void tick();
  public /*open*/ void stopped() { }
  public /*open*/ void focused() { focused = true; }
  public /*open*/ void unfocused() { focused = false; }
  public /*open*/ void hints() { }
  public /*open*/ boolean requiresDraw() { return true; } // just a hint
  
  public Click[] btns = new Click[]{new Click(Click.LEFT), new Click(Click.RIGHT), new Click(Click.CENTER), new Click(Click.BACK), new Click(Click.FORWARD)};
  public abstract void mouseDown(int x, int y, Click c);
  public abstract void mouseUp(int x, int y, Click c);
  public abstract void scroll(float dx, float dy, boolean shift);
  public abstract boolean key(Key key, int scancode, KeyAction action);
  public abstract void typed(int p);
  
  ///////// interaction \\\\\\\\\
  
  public XY windowPos() { return impl.windowPos(); }
  
  public void setTitle(String s) { impl.setTitle(s); }
  public String getTitle() { return impl.getTitle(); }
  
  
  public enum CursorType { REGULAR, HAND, IBEAM }
  public void setCursor(CursorType c) { impl.setCursor(c); }
  
  public void openFile(String filter, Path initial, Consumer<Path> onResult) { impl.openFile(filter, initial, onResult); }
  public void saveFile(String filter, Path initial, Consumer<Path> onResult) { impl.saveFile(filter, initial, onResult); }
  
  public void copyString(String s) { impl.copyString(s); }
  public void pasteString(Consumer<String> f) { impl.pasteString(f); }
  
  public void closeOnNext() { shouldStop.set(true); } // close the window on next tick
  public void closeRequested() { closeOnNext(); } // called when user wants to close window
  
  public Devtools createTools() { return impl.createTools(); }
  public void enqueue(Runnable o) { impl.enqueue(o); }
  
  public enum WindowType {
    NORMAL, POPUP
  }
  public void setType(WindowType t) {
    impl.setType(t);
  }
  
  private boolean hasSetup = false;
  public final AtomicBoolean shouldStop = new AtomicBoolean(false);
  public final AtomicBoolean updateSize = new AtomicBoolean(true);
  public int nodrawFrames=-60;
  public void nextFrame() {
    long sns = System.nanoTime();
    Devtools t = tools;
    if (t!=null) t.timeStart(sns);
    
    
    // important events
    if (!hasSetup) {
      try {
        setup();
      } catch (Throwable e) {
        System.err.println("Errored during setup:");
        closeOnNext();
        hasSetup = true;
        throw new RuntimeException(e);
      }
      hasSetup = true;
    }
    boolean resize = updateSize.getAndSet(false);
    if (resize) {
      resized(impl.runResize());
    }
    
    // regular events
    impl.runEvents();
    for (Click c : btns) c.tick(dx, dy); // process mouse
    eventTick();
    if (t!=null) t.time("event");
    
    // process nodes
    maybeResize();
    tick();
    if (t!=null) t.time("tick");
    maybeResize();
    
    // redraw
    // nodrawFrames++;
    // int toolsRedraw = t!=null? t.redrawInsp() : 0;
    // boolean drawNeeded = ALWAYS_REDRAW || shouldRedraw() || resize || toolsRedraw==2;
    // boolean copyNeeded = USE_OFFSCREEN && (drawNeeded || DEBUG_REDRAW || nodrawFrames<20);
    //
    // impl.startDraw(drawNeeded || copyNeeded);
    // Graphics prim = impl.winG;
    // if (drawNeeded) {
    //   int prevCount = prim.canvas.getSaveCount();
    //   draw(prim, ALWAYS_REDRAW || toolsRedraw!=0 || resize);
    //   if (prim.canvas.getSaveCount() != prevCount) throw new RuntimeException("Unmatched saves and restores");
    //   nodrawFrames = Math.min(nodrawFrames, 0);
    // }
    // if (DEBUG_REDRAW) prim.rect(0, 0, w, h, 0x10000000);
    // if (copyNeeded) offscreen.drawTo(impl.winG.canvas, 0, 0);
    // if (t!=null) t.time("draw");
    //
    // impl.endDraw(drawNeeded);
    // if (t!=null) t.time("flush");
  
    int toolsRedraw = t!=null? t.redrawInsp() : 0;
    boolean draw = resize || requiresDraw() || toolsRedraw==2 || !impl.misuseBuffers || nodrawFrames<20;
    impl.startDraw(draw);
    if (draw) {
      int prevCount = impl.winG.canvas.getSaveCount();
      boolean drew = draw(impl.winG, resize || toolsRedraw!=0);
      if (impl.winG.canvas.getSaveCount() != prevCount) throw new RuntimeException("Unmatched saves and restores");
      
      if (drew) nodrawFrames = Math.min(nodrawFrames, 0);
      else nodrawFrames++;
    }
    if (t!=null) t.time("draw");
    impl.endDraw(draw);
    if (t!=null) t.time("flush");
  
    frameCount++;
    dx = 0; dy = 0;
    if (t!=null) t.time("all", sns);
  }
  
  // deprecated methods for erroring on usage
  @Deprecated public final void resized() { throw new AssertionError(); }
}
