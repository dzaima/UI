package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.utils.*;

import java.nio.file.Path;
import java.util.function.Consumer;

public abstract class WindowImpl {
  public final Window w;
  public WindowInit init;
  
  public static final boolean ALWAYS_REDRAW = false; // must be true if not USE_OFFSCREEN
  public static final boolean USE_OFFSCREEN = true;
  public static boolean DEBUG_REDRAW = false;
  public static boolean ESC_EXIT = false;
  public boolean vsync = true;
  public Windows mgr;
  
  protected WindowImpl(Window w, WindowInit init) {
    this.w = w;
    this.init = init;
  }
  
  ///////// actions \\\\\\\\\
  public abstract void setTitle(String s);
  public abstract void setCursor(Window.CursorType c);
  public abstract XY windowPos();
  public abstract String getTitle();
  public abstract void setVisible(boolean v);
  public abstract void setType(Window.WindowType t);
  public abstract void focus();
  
  public abstract void openFile(String filter, Path initial, Consumer<Path> onResult);
  public abstract void saveFile(String filter, Path initial, Consumer<Path> onResult);
  
  public abstract void copyString(String s);
  public abstract void pasteString(Consumer<String> f);
  
  ///////// window management \\\\\\\\\
  public abstract void start(Windows ws);
  public abstract boolean running();
  
  private static boolean cleaned;
  protected static Vec<Runnable> cleaners = new Vec<>();
  
  public static void cleanup() {
    assert !cleaned;
    cleaned = true;
    for (Runnable c : cleaners) c.run();
  }
  
  public abstract void enqueue(Runnable o);
  public abstract void runEvents();
  
  
  public abstract boolean needsDraw();
  public abstract void startDraw(boolean needed);
  public abstract void endDraw(boolean needed);
  public abstract void runResize();
  
  public Graphics winG = new Graphics();
  public OffscreenGraphics offscreen;
  
  
  
  public final Devtools createTools() {
    try {
      if (w.tools!=null || !(w instanceof NodeWindow) || w instanceof Devtools) return w.tools;
      Devtools dt = Devtools.create((NodeWindow) w);
      mgr.start(dt);
      return dt;
    } catch (Throwable t) {
      System.err.println("Failed creating devtools:");
      t.printStackTrace();
      return null;
    }
  }
}
