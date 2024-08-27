package dzaima.ui.gui.lwjgl;

import dzaima.ui.gui.*;
import dzaima.utils.*;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class LwjglManager extends Windows {
  public LwjglManager() {
  }
  
  protected Rect selfPrimaryDisplay() {
    return LwjglWindow.primaryDisplay();
  }
  
  protected WindowImpl selfMakeWindow(Window w, WindowInit init) {
    return new LwjglWindow(w, init);
  }
  
  public void startSelf(Consumer<Windows> fn) {
    fn.accept(this);
    while (ws.sz > 0) {
      Tools.sleep(1000/120);
      for (Window c : ws) if (!c.impl.running()) ws.remove(c);
      GLFW.glfwPollEvents();
    }
    for (Window c : ws) c.impl.closeOnNext();
    finalWait: while (true) {
      GLFW.glfwPollEvents();
      Tools.sleep(1000/120);
      for (Window c : ws) if (c.impl.running()) continue finalWait;
      break;
    }
  }
}
