package dzaima.ui.gui;

import dzaima.ui.gui.jwm.JWMWindow;
import dzaima.ui.gui.lwjgl.LwjglWindow;
import dzaima.utils.*;
import io.github.humbleui.jwm.App;
import io.github.humbleui.skija.impl.Library;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class Windows {
  public static int MAX_WINDOWS = 10;
  private Windows() { }
  
  final Vec<Window> ws = new Vec<>();
  
  public enum Manager { LWJGL, JWM }
  private static Manager mgr = Manager.LWJGL;
  private static boolean mgrFrozen;
  public static void setManager(Manager m) {
    if (mgrFrozen) throw new RuntimeException("Couldn't change manager because it's already in use");
    mgr = m;
  }
  public static Manager getManager() {
    mgrFrozen = true;
    return mgr;
  }
  
  public static Rect primaryDisplay() {
    switch (getManager()) { default: throw new IllegalStateException();
      case LWJGL:
        return LwjglWindow.primaryDisplay();
      case JWM:
        return JWMWindow.primaryDisplay();
    }
  }
  
  public static WindowImpl makeWindowImpl(Window w, WindowInit init) {
    if ("false".equals(System.getProperty("skija.staticLoad"))) Library.load();
    switch (getManager()) {
      case JWM: return new JWMWindow(w, init);
      case LWJGL: return new LwjglWindow(w, init);
      default: throw new IllegalStateException();
    }
  }
  
  public static void start(Consumer<Windows> fn) {
    Windows w = new Windows();
    switch (getManager()) {
      case JWM: {
        App.start(() -> fn.accept(w));
        // App.init();
        // fn.accept(w);
        // App.start();
        break;
      }
      case LWJGL: {
        fn.accept(w);
        while (w.ws.sz > 0) {
          Tools.sleep(1000/120);
          for (Window c : w.ws) if (!c.impl.running()) w.ws.remove(c);
          GLFW.glfwPollEvents();
        }
        for (Window c : w.ws) c.impl.closeOnNext();
        finalWait: while (true) {
          GLFW.glfwPollEvents();
          Tools.sleep(1000/120);
          for (Window c : w.ws) if (c.impl.running()) continue finalWait;
          break;
        }
        break;
      }
    }
    WindowImpl.cleanup();
  }
  
  public void start(Window w) {
    synchronized (ws) {
      if (ws.sz>=MAX_WINDOWS) throw new RuntimeException("Refusing to create more than Windows.MAX_WINDOWS (= "+MAX_WINDOWS+") windows");
      ws.add(w);
    }
    w.impl.start(this);
  }
  public void stopped(Window w) {
    ws.remove(w);
    if (getManager()==Manager.JWM && ws.size()==0) App.terminate();
  }
}
