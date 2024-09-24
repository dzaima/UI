package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class WindowImpl {
  public final Window w;
  public WindowInit init;
  
  public static boolean ESC_EXIT = false;
  public boolean vsync = true;
  public Windows mgr;
  public final boolean misuseBuffers;
  
  protected WindowImpl(Window w, WindowInit init, boolean misuseBuffers) {
    this.w = w;
    this.init = init;
    this.misuseBuffers = misuseBuffers;
  }
  
  ///////// actions \\\\\\\\\
  public abstract void setTitle(String s);
  public abstract void setCursor(Window.CursorType c);
  public abstract XY windowPos();
  public abstract String getTitle();
  public abstract void setVisible(boolean v);
  public abstract void setType(Window.WindowType t);
  public abstract void focus();
  
  public abstract void openFolder(Path initialDir, Consumer<Path> onResult);
  public abstract void openFile(String filter, Path initialDir, Consumer<Path> onResult);
  public abstract void saveFile(String filter, Path initialDir, String initialName, Consumer<Path> onResult);
  
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
  
  public abstract void enqueue(Runnable r); // can be called from any thread; runs r during next tick
  public abstract void runOnUIThread(Runnable r); // can be called from any thread; runs r at an arbitrary point on the UI thread (i.e. always can be replaced with enqueue, but not the other way around)
  public abstract void runEvents();
  public abstract boolean intentionallyLong();
  
  
  public MainGraphics winG = new MainGraphics();
  public abstract void draw(Runnable draw);
  public abstract void runResize();
  
  public final AtomicBoolean shouldStop = new AtomicBoolean(false);
  public abstract void closeOnNext();
  public abstract boolean requiresDraw();
  
  
  
  public final Devtools createTools() {
    try {
      if (w.tools!=null || w instanceof Devtools) return w.tools;
      Devtools dt = Devtools.create(w);
      mgr.start(dt);
      return dt;
    } catch (Throwable t) {
      Log.error("WindowImpl", "Failed creating devtools:");
      Log.stacktrace("WindowImpl", t);
      return null;
    }
  }
}
