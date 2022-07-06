package dzaima.ui.gui.jwm;

import dzaima.ui.gui.Window.DrawReq;
import dzaima.ui.gui.*;
import dzaima.ui.gui.lwjgl.LwjglWindow;
import dzaima.utils.*;
import dzaima.utils.Log;
import io.github.humbleui.jwm.Window;
import io.github.humbleui.jwm.*;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.IRect;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class JWMWindow extends WindowImpl {
  public static final boolean DEBUG_UPDATES = false;
  
  public final int id; // roughly unique identifier of the current window (as long as your program hasn't made 4 billion windows)
  public Window jwmw;
  public String title;
  public boolean visible;
  
  private static final AtomicInteger idCounter = new AtomicInteger();
  public JWMWindow(dzaima.ui.gui.Window w, WindowInit init) {
    super(w, init, false);
    id = idCounter.getAndIncrement();
  }
  
  static {
    io.github.humbleui.jwm.Log.setLogger(o -> Log.info("JWM", Objects.toString(o)));
    Log.onLogLevelChanged(() -> io.github.humbleui.jwm.Log.setVerbose(Log.level.i <= Log.Level.FINE.i));
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
      default:
      case REGULAR: jwmw.setMouseCursor(MouseCursor.ARROW); break;
      case HAND: jwmw.setMouseCursor(MouseCursor.POINTING_HAND); break;
      case IBEAM: jwmw.setMouseCursor(MouseCursor.IBEAM); break;
      case CROSSHAIR: jwmw.setMouseCursor(MouseCursor.CROSSHAIR); break;
      case E_RESIZE: case W_RESIZE: case EW_RESIZE: jwmw.setMouseCursor(MouseCursor.RESIZE_WE); break;
      case N_RESIZE: case S_RESIZE: case NS_RESIZE: jwmw.setMouseCursor(MouseCursor.RESIZE_NS); break;
      case NE_RESIZE: case SW_RESIZE: case NESW_RESIZE: jwmw.setMouseCursor(MouseCursor.RESIZE_NESW); break;
      case NW_RESIZE: case SE_RESIZE: case NWSE_RESIZE: jwmw.setMouseCursor(MouseCursor.RESIZE_NWSE); break;
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
  public SkijaLayerGL layer;
  void make() {
    assert state.get()==0;
    state.set(1);
    w.mx=-99999; w.my=-99999;
    
    jwmw = App.makeWindow();
    eh = new JWMEventHandler(this);
    jwmw.setEventListener(eh);
    layer = new SkijaLayerGL(this);
    layer.attach(jwmw);
    
    
    setTitle(init.title);
    setType(init.type);
    Rect r = init.rect;
    if (r==null) {
      Rect d = WindowInit.defaultExtraRect();
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
    winG.setSurface(s);
    w.w = winG.w;
    w.h = winG.h;
  }
  
  ///////// runtime \\\\\\\\\
  private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
  public void enqueue(Runnable r) {
    queue.add(r);
    requestTick();
  }
  
  public boolean needsDraw() {
    return layer.uninitialized;
  }
  public void startDraw(boolean needed) {
    if (needed) layer.beforePaint();
  }
  public void endDraw(boolean needed) {
    if (needed) layer.afterPaint();
    else layer.skipPaint();
  }
  
  public Surface runResize() {
    return layer.initParts();
  }
  
  public void closeOnNext() {
    shouldStop.set(true);
    jwmw.requestFrame();
  }
  
  public static Rect primaryDisplay() {
    return new Rect(App.getPrimaryScreen().getBounds());
  }
  
  private boolean intentionallyLong;
  private DrawReq drawRequest;
  private long prevFrameStartNs = -1;
  private long prevFrameStartMs = -1;
  
  public void nextFrame(boolean onlyTick) { // if !onlyTick, return 
    assert state.get()==1;
    if (drawRequest==null) {
      prevFrameStartNs = System.nanoTime();
      prevFrameStartMs = System.currentTimeMillis();
      nextRequestMs = Long.MAX_VALUE;
    }
    if (!onlyTick) cancelTickRequest(); // something force-called a draw while a tick request is active; ¯\_(ツ)_/¯
    
    DrawReq pdr = drawRequest;
    if (drawRequest==null) {
      try {
        drawRequest = w.nextTick();
      } catch (Throwable e) {
        dzaima.ui.gui.Window.onFrameError(w, e);
        return;
      }
    }
    
    if (DEBUG_UPDATES) System.out.println(Time.logStart(id)+"nextFrame("+onlyTick+"): drawRequest="+pdr+"→"+drawRequest);
    boolean draw = drawRequest!=DrawReq.NONE;
    
    intentionallyLong = !draw;
    if (draw) {
      try {
        jwmw.requestFrame();
        if (onlyTick) return; // and have next run through nextFrame continue to postDraw
        w.nextDraw(winG, drawRequest==DrawReq.FULL);
      } catch (Throwable e) {
        dzaima.ui.gui.Window.onFrameError(w, e);
      }
    } else {
      requestTick(false);
    }
    
    w.postDraw(draw, prevFrameStartNs);
    
    drawRequest = null;
  }
  
  
  
  private final Timer timer = new Timer(true);
  private TimerTask currentTickRequest; // null while jwm.Window.requestFrame is definitely active; otherwise, state is arbitrary
  private long nextRequestMs = Long.MAX_VALUE;
  private void requestTick(boolean instant) {
    long next = prevFrameStartMs + (instant? 1000/w.framerate() : w.tickDelta());
    if (!instant || next < nextRequestMs) {
      nextRequestMs = next;
      if (DEBUG_UPDATES) System.out.println(Time.logStart(id) + "scheduling tick to "+Instant.ofEpochMilli(next));
      
      cancelTickRequest();
      timer.schedule(currentTickRequest = new TimerTask() {
        public void run() {
          if (DEBUG_UPDATES) System.out.println(Time.logStart(id) + "tick timer hit");
          App.runOnUIThread(() -> {
            if (currentTickRequest==this && !shouldStop.get()) {
              currentTickRequest = null;
              nextFrame(true);
            }
          });
        }
      }, new Date(next));
    }
  }
  public void cancelTickRequest() {
    if (currentTickRequest!=null) {
      currentTickRequest.cancel();
      currentTickRequest = null;
    }
  }
  public void requestTick() {
    if (currentTickRequest==null) return; // else, requestFrame is running
    requestTick(true);
  }
  
  public void runEvents() {
    while (true) {
      Runnable r = queue.poll();
      if (r==null) break;
      r.run();
    }
  }
  
  public boolean intentionallyLong() {
    return intentionallyLong;
  }
  
  
  private boolean stopped;
  public void stop() {
    if (stopped) return;
    stopped = true;
    if (w.hijack!=null) w.hijack.hStopped();
    setVisible(false);
    startDraw(true);
    w.stopped();
    endDraw(true);
    
    layer.close();
    jwmw.close();
    jwmw = null; // make sure nothing accidentally uses it
    
    state.set(2);
    mgr.stopped(w);
  }
}