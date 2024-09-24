package dzaima.ui.gui;

import dzaima.utils.*;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public abstract class Windows {
  public static int MAX_WINDOWS = 10;
  
  protected final Vec<Window> ws = new Vec<>();
  protected Windows() { }
  
  public enum Manager { LWJGL, JWM }
  private static Manager mgrType = Manager.JWM;
  private static Windows mgrSelected;
  public static void setManager(Manager m) {
    if (mgrSelected!=null) throw new RuntimeException("Couldn't change manager because it's already in use");
    mgrType = m;
  }
  public static Manager getManagerType() {
    getManager();
    return mgrType;
  }
  public static Windows getManager() {
    freezeManager();
    return mgrSelected;
  }
  public static void freezeManager() {
    if (mgrSelected!=null) return;
    try {
      String className;
      switch (mgrType) { default: throw new IllegalStateException();
        case JWM:   className = "dzaima.ui.gui.jwm.JWMManager"; break;
        case LWJGL: className = "dzaima.ui.gui.lwjgl.LwjglManager"; break;
      }
      Class<? extends Windows> cl = Class.forName(className).asSubclass(Windows.class);
      mgrSelected = cl.getConstructor().newInstance();
    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Rect primaryDisplay() {
    return getManager().selfPrimaryDisplay();
  }
  protected abstract Rect selfPrimaryDisplay();
  
  public static WindowImpl makeWindowImpl(Window w, WindowInit init) {
    return getManager().selfMakeWindow(w, init);
  }
  protected abstract WindowImpl selfMakeWindow(Window w, WindowInit init);
  
  public static void start(Consumer<Windows> fn) {
    getManager().startSelf(fn);
    WindowImpl.cleanup();
  }
  protected abstract void startSelf(Consumer<Windows> fn);
  
  public void start(Window w) {
    synchronized (ws) {
      if (ws.sz>=MAX_WINDOWS) throw new RuntimeException("Refusing to create more than Windows.MAX_WINDOWS (= "+MAX_WINDOWS+") windows");
      ws.add(w);
    }
    w.impl.start(this);
  }
  public void stopped(Window w) {
    ws.remove(w);
  }
}
