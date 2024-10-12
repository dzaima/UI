package dzaima.ui.gui.lwjgl;

import dzaima.ui.gui.Window;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.utils.*;
import io.github.humbleui.skija.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

@SuppressWarnings("ConstantConditions")
public class LwjglWindow extends WindowImpl {
  
  public String title;
  public long windowPtr;
  
  
  public LwjglWindow(Window w, WindowInit init) {
    super(w, init, true);
  }
  
  
  static {
    if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
    GLFWErrorCallback.createPrint(System.err).set();
  }
  public static void initializeStatic() { }
  
  
  protected final AtomicInteger state = new AtomicInteger(); // 0 - not started; 1 - started; 2 - closed
  public void start(Windows mgr) {
    this.mgr = mgr;
    assert state.get()==0;
    Tools.thread(() -> {
      make();
      while (true) {
        try {
          if (shouldStop.get()) break;
          nextFrame();
        } catch (Throwable t) {
          Tools.sleep(1000/60);
          Window.onFrameError(w, t);
        }
      }
      stop();
    });
  }
  
  public boolean running() {
    return state.get()!=2;
  }
  
  
  
  
  
  
  ///////// setup \\\\\\\\\
  void make() {
    assert state.get()==0;
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    if (init.type!=Window.WindowType.POPUP) {
      glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
      title = init.title;
    } else {
      glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
      glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
      // glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
      glfwWindowHint(GLFW_FLOATING, GLFW_TRUE);
      glfwInitHint(GLFW_COCOA_MENUBAR, GLFW_FALSE);
      title = "Menu";
    }
    String appClass = initAppClass();
    glfwWindowHintString(GLFW_X11_CLASS_NAME, appClass);
    glfwWindowHintString(GLFW_X11_INSTANCE_NAME, appClass);
    w.hints();
    
    Rect r = init.rect;
    if (r==null) r = primaryDisplay();
    
    windowPtr = glfwCreateWindow(r.w(), r.h(), title, NULL, NULL);
    if (windowPtr == NULL) throw new RuntimeException("Failed to create the GLFW window");
    
    
    glfwSetWindowPos(windowPtr, r.sx, r.sy);
    
    glfwMakeContextCurrent(windowPtr);
    glfwSwapInterval(vsync? 1 : 0);
    if (init.visible) glfwShowWindow(windowPtr);
    GL.createCapabilities();
    context = DirectContext.makeGL();
    
    glfwSetWindowSizeCallback(windowPtr, (cw, nw, nh) -> { // no need to queue, we got atomics
      if (state.get()!=1) return;
      w.updateSize.set(true);
    });
    glfwSetCursorPosCallback(windowPtr, (cw, mxf, myf) -> enqueue(() -> {
      int nmx = (int) mxf;
      int nmy = (int) myf;
      w.dx+= nmx-w.mx;
      w.dy+= nmy-w.my;
      w.mx = nmx;
      w.my = nmy;
    }));
    glfwSetMouseButtonCallback(windowPtr, (cw, btn, action, mods) -> enqueue(() -> {
      Click c = w.btns[btn];
      c.down = action==GLFW_PRESS;
      if (c.down) w.mouseDown(c);
      else w.mouseUp(w.mx, w.my, c);
    }));
    glfwSetScrollCallback(windowPtr, (cw, dx, dy) -> enqueue(() -> w.scroll(100*(float) dx, 100*(float) dy, Key.shift(mod))));
    glfwSetCharCallback(windowPtr, (cw, p) -> enqueue(() -> w.typed(p)));
    glfwSetKeyCallback(windowPtr, (cw, key, scancode, action, mod) -> enqueue(() -> {
      if (key==GLFW_KEY_LEFT_SHIFT   || key==GLFW_KEY_RIGHT_SHIFT  ) this.mod = action==GLFW_PRESS? mod|Key.M_SHIFT : mod&~Key.M_SHIFT; // shift = action==GLFW_PRESS? true : action==GLFW_RELEASE? false : shift;
      if (key==GLFW_KEY_LEFT_CONTROL || key==GLFW_KEY_RIGHT_CONTROL) this.mod = action==GLFW_PRESS? mod|Key.M_CTRL  : mod&~Key.M_CTRL; // ctrl  = action==GLFW_PRESS? true : action==GLFW_RELEASE? false : ctrl ;
      if (key==GLFW_KEY_LEFT_ALT     || key==GLFW_KEY_RIGHT_ALT    ) this.mod = action==GLFW_PRESS? mod|Key.M_ALT   : mod&~Key.M_ALT; // alt   = action==GLFW_PRESS? true : action==GLFW_RELEASE? false : alt  ;
      for (Click c : w.btns) c.mod0 = this.mod;
      KeyVal kv;
      int kmod = mod&Key.M_MOD;
      if (key>=GLFW_KEY_0 & key<=GLFW_KEY_9) kv = KeyVal.valueOf("d" + (key-GLFW_KEY_0));
      else if (key>=GLFW_KEY_A && key<=GLFW_KEY_Z) kv = KeyVal.valueOf(String.valueOf((char)('a' + (key-GLFW_KEY_A))));
      else if (key>=GLFW_KEY_KP_0 & key<=GLFW_KEY_KP_9) { kv = KeyVal.valueOf("d" + (key-GLFW_KEY_KP_0)); kmod|= Key.P_KP; }
      else if (key>=GLFW_KEY_F1 && key<=GLFW_KEY_F25) kv = KeyVal.valueOf("f" + (key-GLFW_KEY_F1+1));
      else switch (key) {
          case GLFW_KEY_SPACE:        kv=KeyVal.space; break;
          case GLFW_KEY_APOSTROPHE:   kv=KeyVal.quote; break;
          case GLFW_KEY_COMMA:        kv=KeyVal.comma; break;
          case GLFW_KEY_MINUS:        kv=KeyVal.minus; break;
          case GLFW_KEY_PERIOD:       kv=KeyVal.period; break;
          case GLFW_KEY_SLASH:        kv=KeyVal.slash; break;
          case GLFW_KEY_SEMICOLON:    kv=KeyVal.semicolon; break;
          case GLFW_KEY_EQUAL:        kv=KeyVal.equal; break;
          case GLFW_KEY_LEFT_BRACKET: kv=KeyVal.openBrak; break;
          case GLFW_KEY_BACKSLASH:    kv=KeyVal.backslash; break;
          case GLFW_KEY_RIGHT_BRACKET:kv=KeyVal.closeBrak; break;
          case GLFW_KEY_GRAVE_ACCENT: kv=KeyVal.backtick; break;
          case GLFW_KEY_ESCAPE:       kv=KeyVal.esc; break;
          case GLFW_KEY_ENTER:        kv=KeyVal.enter; break;
          case GLFW_KEY_TAB:          kv=KeyVal.tab; break;
          case GLFW_KEY_BACKSPACE:    kv=KeyVal.backspace; break;
          case GLFW_KEY_INSERT:       kv=KeyVal.ins; break;
          case GLFW_KEY_DELETE:       kv=KeyVal.del; break;
          case GLFW_KEY_RIGHT:        kv=KeyVal.right; break;
          case GLFW_KEY_LEFT:         kv=KeyVal.left; break;
          case GLFW_KEY_DOWN:         kv=KeyVal.down; break;
          case GLFW_KEY_UP:           kv=KeyVal.up; break;
          case GLFW_KEY_PAGE_UP:      kv=KeyVal.pgup; break;
          case GLFW_KEY_PAGE_DOWN:    kv=KeyVal.pgdn; break;
          case GLFW_KEY_HOME:         kv=KeyVal.home; break;
          case GLFW_KEY_END:          kv=KeyVal.end; break;
          case GLFW_KEY_CAPS_LOCK:    kv=KeyVal.capsLock; break;
          case GLFW_KEY_SCROLL_LOCK:  kv=KeyVal.scrollLock; break;
          case GLFW_KEY_NUM_LOCK:     kv=KeyVal.numLock; break;
          case GLFW_KEY_PRINT_SCREEN: kv=KeyVal.printScreen; break;
          case GLFW_KEY_PAUSE:        kv=KeyVal.pause; break;
          case GLFW_KEY_KP_DECIMAL:   kv=KeyVal.period;   kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_DIVIDE:    kv=KeyVal.slash;    kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_MULTIPLY:  kv=KeyVal.multiply; kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_SUBTRACT:  kv=KeyVal.minus;    kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_ADD:       kv=KeyVal.add;      kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_ENTER:     kv=KeyVal.enter;    kmod|= Key.P_KP; break;
          case GLFW_KEY_KP_EQUAL:     kv=KeyVal.equal;    kmod|= Key.P_KP; break;
          case GLFW_KEY_LEFT_SHIFT:   kv=KeyVal.shift; break;
          case GLFW_KEY_LEFT_CONTROL: kv=KeyVal.ctrl; break;
          case GLFW_KEY_LEFT_ALT:     kv=KeyVal.alt; break;
          case GLFW_KEY_LEFT_SUPER:   kv=KeyVal.meta; break;
          case GLFW_KEY_RIGHT_SHIFT:  kv=KeyVal.shift; kmod|= Key.P_RIGHT; break;
          case GLFW_KEY_RIGHT_CONTROL:kv=KeyVal.ctrl;  kmod|= Key.P_RIGHT; break;
          case GLFW_KEY_RIGHT_ALT:    kv=KeyVal.alt;   kmod|= Key.P_RIGHT; break;
          case GLFW_KEY_RIGHT_SUPER:  kv=KeyVal.meta;  kmod|= Key.P_RIGHT; break;
          case GLFW_KEY_MENU:         kv=KeyVal.menu; break;
          case -1:                    kv=KeyVal.unknown; break;
          // case GLFW_KEY_WORLD_1:      k=Key.world_1; break;
          // case GLFW_KEY_WORLD_2:      k=Key.world_2; break;
          default:
            Log.warn("LWJGL", "Unhandled key "+key);
            kv=KeyVal.unknown;
      }
      w.modifiers(kmod);
      if (!w.key(new Key(kv, kmod), scancode, action==GLFW_PRESS? KeyAction.PRESS : action==GLFW_RELEASE? KeyAction.RELEASE : KeyAction.REPEAT)) {
        if (key==GLFW_KEY_ESCAPE && action==GLFW_PRESS && ESC_EXIT) glfwSetWindowShouldClose(cw, true);
      }
    }));
    glfwSetWindowFocusCallback(windowPtr, (cw, focus) -> enqueue(() -> {
      if (focus) w.focused();
      else w.unfocused();
    }));
    
    newCanvas();
    w.mx=w.w*2; w.my=w.h*2;
    glfwPollEvents();
    context.flush();
    glfwSwapBuffers(windowPtr);
    glfwPollEvents();
    state.set(1);
  }
  
  
  private DirectContext context;
  private BackendRenderTarget renderTarget;
  private Surface surface;
  
  private void newCanvas() {
    int[] wTmp = new int[1]; float[] xscTmp = new float[1];
    int[] hTmp = new int[1]; float[] yscTmp = new float[1];
    glfwGetFramebufferSize(windowPtr, wTmp, hTmp);
    glfwGetWindowContentScale(windowPtr, xscTmp, yscTmp);
    w.w = wTmp[0]; w.h = hTmp[0];
    w.dpi = xscTmp[0];
    
    if (surface!=null) surface.close();
    if (renderTarget!=null) renderTarget.close();
    
    renderTarget = BackendRenderTarget.makeGL(w.w, w.h, /*samples*/ 0, /*stencil*/ 8, GL11.glGetInteger(0x8CA6), FramebufferFormat.GR_GL_RGBA8); // magic
    surface = Surface.wrapBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB(), new SurfaceProps(PixelGeometry.RGB_H));
    winG.setSurface(surface);
  }
  
  
  
  ///////// runtime \\\\\\\\\
  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  public void enqueue(Runnable r) {
    queue.add(r);
  }
  public void runOnUIThread(Runnable r) {
    queue.add(r);
  }
  
  public boolean requiresDraw() { return false; }
  
  public void draw(Runnable draw) {
    draw.run();
  }
  public void skipDraw() {
    draw(() -> {});
  }
  
  public void runResize() {
    newCanvas();
  }
  
  public void closeOnNext() {
    shouldStop.set(true);
  }
  
  static Rect primaryDisplay;
  public static Rect primaryDisplay() {
    if (primaryDisplay == null) {
      initializeStatic();
      long ptr = glfwGetPrimaryMonitor();
      GLFWVidMode vidmode = glfwGetVideoMode(ptr);
      int w = vidmode.width();
      int h = vidmode.height();
      primaryDisplay = new Rect(0, 0, w, h);
    }
    return primaryDisplay;
  }
  
  
  public void runEvents() {
    while (true) {
      Runnable r = queue.poll();
      if (r==null) break;
      r.run();
    }
  }
  public boolean intentionallyLong() {
    return false;
  }
  
  public int mod;
  public void nextFrame() {
    long sns = System.nanoTime();
    assert state.get()==1;
    glfwMakeContextCurrent(windowPtr);
    if (glfwWindowShouldClose(windowPtr)) shouldStop.set(true);
    Window.DrawReq r = w.nextTick();
    boolean draw = r!=Window.DrawReq.NONE;
    if (draw) {
      w.nextDraw(winG, r==Window.DrawReq.FULL);
    } else {
      skipDraw();
    }
    
    context.flush();
    glfwSwapBuffers(windowPtr);
    
    w.postDraw(draw, sns);
  }
  
  public void stop() {
    if (w.hijack!=null) w.hijack.hStopped();
    glfwMakeContextCurrent(windowPtr);
    w.stopped(); // TODO this calls Windows::remove, which isn't synchronized
    glfwDestroyWindow(windowPtr);
    Callbacks.glfwFreeCallbacks(windowPtr);
    state.set(2);
  }
  
  
  
  public void setTitle(String s) {
    if (!s.equals(title)) {
      title = s;
      if (windowPtr!=0L) GLFW.glfwSetWindowTitle(windowPtr, s);
    }
  }
  public String getTitle() {
    return title;
  }
  
  public void setVisible(boolean v) {
    if (v) glfwShowWindow(windowPtr);
    else glfwHideWindow(windowPtr);
  }
  
  public final HashMap<Window.CursorType, Long> cursors = new HashMap<>();
  public void setCursor(Window.CursorType c) {
    Long p;
    p = cursors.get(c);
    if (p==null) {
      int t;
      switch (c) {
        default:
        case REGULAR: t = GLFW_ARROW_CURSOR; break;
        case HAND: t = GLFW_HAND_CURSOR; break;
        case IBEAM: t = GLFW_IBEAM_CURSOR; break;
        case CROSSHAIR: t = GLFW_CROSSHAIR_CURSOR; break;
        case E_RESIZE: case W_RESIZE: case EW_RESIZE: t = GLFW_RESIZE_EW_CURSOR; break;
        case N_RESIZE: case S_RESIZE: case NS_RESIZE: t = GLFW_RESIZE_NS_CURSOR; break;
        case NW_RESIZE: case SE_RESIZE: case NWSE_RESIZE: t = GLFW_RESIZE_NWSE_CURSOR; break;
        case NE_RESIZE: case SW_RESIZE: case NESW_RESIZE: t = GLFW_RESIZE_NESW_CURSOR; break;
      }
      p = glfwCreateStandardCursor(t);
      cursors.put(c, p);
    }
    GLFW.glfwSetCursor(windowPtr, p);
  }
  
  public void openFolder(Path initialDir, Consumer<Path> onResult) { NFD.openFolderStatic(initialDir, onResult); }
  public void openFile(String filter, Path initialDir, Consumer<Path> onResult) { NFD.openFileStatic(filter, initialDir, onResult); }
  public void saveFile(String filter, Path initialDir, String initialName, Consumer<Path> onResult) { NFD.saveFileStatic(filter, initialDir, onResult); }
  
  
  public void copyString(String s) {
    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection ss = new StringSelection(s);
    cb.setContents(ss, ss);
  }
  public void pasteString(Consumer<String> f) {
      try {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        f.accept((String) cb.getData(DataFlavor.stringFlavor));
      } catch (Exception e) {
        Log.stacktrace("LWJGL pasteString", e);
        f.accept(null);
      }
  }
  
  public XY windowPos() {
    int[] x=new int[1], y=new int[1];
    GLFW.glfwGetWindowPos(windowPtr, x, y);
    return new XY(x[0], y[0]);
  }
  
  public void setType(Window.WindowType t) { }
  
  public void focus() {
    glfwFocusWindow(windowPtr);
  }
  
  
  private static void lwjglCleanup() {
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }
  static {
    WindowImpl.cleaners.add(LwjglWindow::lwjglCleanup);
  }
}
