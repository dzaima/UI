package dzaima.ui.gui.jwm;

import dzaima.ui.gui.*;
import dzaima.ui.gui.lwjgl.LwjglWindow;
import dzaima.utils.*;
import io.github.humbleui.jwm.Window;
import io.github.humbleui.jwm.*;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.IRect;

import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class JWMWindow extends WindowImpl {
  public Window jwmw;
  public String title;
  public boolean visible;
  
  
  public JWMWindow(dzaima.ui.gui.Window w, WindowInit init) {
    super(w, init);
  }
  
  static {
    App.init();
  }
  
  public void setTitle(String s) {
    if (!s.equals(title)) {
      title = s;
      jwmw.setTitle(s);
    }
  }
  public String getTitle() {
    return title;
  }
  
  public void setVisible(boolean v) {
    visible = v;
    jwmw.setVisible(v);
  }
  
  public void setCursor(dzaima.ui.gui.Window.CursorType c) {
    switch (c) {
      case REGULAR:
        jwmw.setMouseCursor(MouseCursor.ARROW);
        break;
      case HAND:
        jwmw.setMouseCursor(MouseCursor.POINTING_HAND);
        break;
      case IBEAM:
        jwmw.setMouseCursor(MouseCursor.IBEAM);
        break;
    }
  }
  // TODO don't use LWJGL for this
  public void openFile(String filter, Path initial, Consumer<Path> onResult) { LwjglWindow.openFileStatic(filter, initial, onResult); }
  public void saveFile(String filter, Path initial, Consumer<Path> onResult) { LwjglWindow.saveFileStatic(filter, initial, onResult); }
  
  public void copyString(String s) {
    Clipboard.set(ClipboardEntry.makePlainText(s));
  }
  
  public void pasteString(Consumer<String> f) {
    ClipboardEntry r = Clipboard.get(ClipboardFormat.TEXT);
    if (r==null) f.accept(null);
    else f.accept(r.getString());
  }
  
  public XY windowPos() {
    IRect r = jwmw.getContentRectAbsolute();
    return new XY(r.getLeft(), r.getTop());
  }
  
  public void setType(dzaima.ui.gui.Window.WindowType t) {
    // if (jwmw instanceof WindowX11) {
    //   ((WindowX11) jwmw)._nDecorations(t==dzaima.ui.gui.Window.WindowType.POPUP? false : true);
    // }
  }
  
  public void focus() {
    // jwmw.focus();
  }
  
  
  protected final AtomicInteger state = new AtomicInteger(); // 0 - not started; 1 - started; 2 - closed
  public void start(Windows mgr) {
    this.mgr = mgr;
    assert state.get()==0;
    App.runOnUIThread(() -> {
      make();
      jwmw.requestFrame();
    });
  }
  
  public boolean running() {
    return state.get()!=2;
  }
  
  
  ///////// setup \\\\\\\\\
  private JWMEventHandler eh;
  void make() {
    assert state.get()==0;
    state.set(1);
    w.mx=-99999; w.my=-99999;
    
    jwmw = App.makeWindow();
    eh = new JWMEventHandler(this);
    jwmw.setEventListener(eh);
    
    
    setTitle(init.title);
    setType(init.type);
    Rect r = init.rect;
    if (r==null) {
      Rect d = Windows.defaultWindowRect();
      jwmw.setWindowPosition(d.sx, d.sy);
      jwmw.setWindowSize(d.w(), d.h());
      jwmw.maximize();
    } else {
      jwmw.setWindowPosition(r.sx, r.sy);
      jwmw.setWindowSize(r.w(), r.h());
      w.w = r.w();
      w.h = r.h();
    }
    
    
    eh.dontPaint = false;
    if (init.visible) setVisible(true);
  }
  
  
  public void newCanvas(Surface s) {
    winG.init(s);
    w.w = winG.w;
    w.h = winG.h;
    if (USE_OFFSCREEN) {
      if (offscreen!=null) offscreen.close();
      offscreen = new OffscreenGraphics(s, w.w, w.h);
    }
  }
  
  ///////// runtime \\\\\\\\\
  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  public void enqueue(Runnable r) {
    queue.add(r);
  }
  
  public boolean needsDraw() {
    return eh.layer.uninitialized;
  }
  public void startDraw(boolean needed) {
    if (needed) eh.layer.beforePaint();
  }
  public void endDraw(boolean needed) {
    if (needed) eh.layer.afterPaint();
    else eh.layer.skipPaint();
  }
  
  public void runResize() { }
  
  public static Rect primaryDisplay() {
    return new Rect(App.getPrimaryScreen().getBounds());
  }
  
  public void nextFrame() {
    assert state.get()==1;
    w.nextFrame();
  }
  public void runEvents() {
    while (true) {
      Runnable r = queue.poll();
      if (r==null) break;
      r.run();
    }
  }
  
  
  
  private boolean stopped;
  public void stop() {
    if (stopped) return;
    stopped = true;
    if (w.tools!=null) w.tools.stoppedInsp();
    setVisible(false);
    startDraw(true);
    w.stopped();
    endDraw(true);
    
    if (offscreen!=null) offscreen.close();
    eh.close();
    jwmw.close();
    jwmw = null; // make sure nothing accidentally uses it
    
    state.set(2);
    mgr.stopped(w);
  }
}