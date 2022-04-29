package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.types.StringNode;
import dzaima.utils.*;
import io.github.humbleui.skija.Surface;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class NodeWindow extends Window {
  public static boolean PRINT_ON_RESIZE = false;
  public static final boolean ALWAYS_REDRAW = false; // must be true if not USE_OFFSCREEN
  public static final boolean USE_OFFSCREEN = true;
  
  public final GConfig gc;
  public final Ctx.WindowCtx ctx;
  public final Node base;
  public final Vec<VirtualWindow> vws = new Vec<>();
  public VirtualWindow focusedVW, hoveredVW;
  
  public NodeWindow(GConfig gc, Ctx pctx, PNodeGroup g, WindowInit i) {
    super(i);
    this.gc = gc;
    NodeVW baseVW = new NodeVW(this, gc, pctx, g) {
      public boolean fullyOpaque() { return true; }
      public boolean drawShadow() { return false; }
      public boolean ownsXY(int x, int y) { return true; }
      public boolean shouldRemove() { return false; }
      public Rect getLocation(int pw, int ph) { return new Rect(0, 0, pw, ph); }
    };
    vws.add(baseVW);
    base = baseVW.base;
    focusedVW = baseVW;
    hoveredVW = baseVW;
    ctx = (Ctx.WindowCtx) base.ctx;
  }
  
  public void setup() {
    for (VirtualWindow vw : vws) vw.started();
    gc.ws.add(this); // TODO not
  }
  
  public void addVW(VirtualWindow vw) {
    vws.add(vw);
    if (setupDone) { // TODO don't
      vw.started();
      vw.newSurface(lastSurface, w, h);
      requestDraw = true;
    }
  }
  
  public void stopped() {
    gc.ws.remove(this);
    for (VirtualWindow c : vws) c.stopped();
  }
  
  
  
  ///////// focus \\\\\\\\\
  public Node focusNode;
  public void focus(Node n) {
    Node prev = focusNode();
    focusNode = n;
    if (prev != null) prev.focusE();
    if (n == null) return;
    
    NodeVW vw = n.ctx.vw();
    if (vw.base.visible) {
      assert n.visible;
      n.focusS();
      focusedVW = vw;
    }
  }
  public void focusVW(VirtualWindow vw) {
    assert vws.indexOf(vw)!=-1;
    focusedVW = vw;
  }
  public Node focusNode() {
    if (focusNode!=null && !focusNode.visible) focusNode = null;
    return focusNode;
  }
  
  
  
  ///////// selection \\\\\\\\\
  public Selection selection;
  private Position selStart;
  public void startSelection(Position p) {
    if (selection!=null) {
      selection.end();
      selection = null;
    }
    selStart = p;
  }
  public void continueSelection(Position p) {
    if (selStart==null) return;
    Selection psel = selection;
    Selection nsel = Position.select(selStart, p);
    if (psel==null || nsel==null || psel.c!=nsel.c || !psel.aS.equals(nsel.aS) || !psel.bS.equals(nsel.bS)) {
      if (psel!=null) psel.end();
      if (nsel!=null) nsel.start();
      selection = nsel;
    }
  }
  public void endSelection() {
    if (selection!=null) selection.end();
    selection = null;
    selStart = null;
  }
  public XY selectionRange(StringNode n) {
    if ((n.flags&StringNode.FL_SEL)==0) return null;
    return selectionRange2(n);
  }
  private XY selectionRange2(StringNode n) {
    if (selection==null) return null;
    PosPart sS = selection.sS; boolean sB = sS.ln == n;
    PosPart eS = selection.eS; boolean eB = eS.ln == n;
    if (sB && eB) return new XY(sS.pos, eS.pos);
    if (sB) return new XY(sS.pos, n.s.length());
    if (eB) return new XY(0, eS.pos);
    return new XY(0, n.s.length());
  }
  
  
  
  ///////// events \\\\\\\\\
  private CursorType lastCursor = CursorType.REGULAR;
  public void eventTick() {
    for (int i = vws.size()-1; i>=0; i--) {
      VirtualWindow v = vws.get(i);
      if (v.rect.contains(mx, my) && v.ownsXY(mx-v.rect.sx, my-v.rect.sy)) {
        hoveredVW = v;
        break;
      }
    }
    for (VirtualWindow vw : vws) vw.eventTick();
    CursorType nCursor = hoveredVW.cursorType();
    if (nCursor!=lastCursor) {
      setCursor(nCursor);
      lastCursor = nCursor;
    }
    vws.filterInplace(c -> {
      if (c.shouldRemove()) {
        c.stopped();
        if (focusedVW==c) {
          focusedVW = vws.get(0);
          if (focusNode!=null && focusNode.ctx.vw()==c) focus(null);
        }
        requestDraw = true;
        return false;
      }
      return true;
    });
  }
  public void tick() {
    gc.tick(this, impl.intentionallyLong());
    for (VirtualWindow c : vws) c.tick();
  }
  
  public void mouseDown(Click cl) {
    if (tools!=null && tools.mouseDownInsp(cl)) return;
    focusedVW = hoveredVW;
    cl.startClick();
    focusedVW.mouseStart(cl);
    cl.nextItem();
    focusedVW.initialMouseTick(cl);
  }
  public void mouseUp(int x, int y, Click cl) {
    cl.endClick();
    if (cl.queueWasEmpty() && gc.isClick(cl)) focus(null);
  }
  
  public void scroll(float dx, float dy, boolean shift) {
    if (shift) {
      dx = dy;
      dy = 0;
    }
    float speed = gc.getProp("scroll.globalSpeed").f();
    hoveredVW.scroll(dx*speed, dy*speed);
  }
  public boolean key(Key key, int scancode, KeyAction a) {
    Node n = focusNode();
    if (n!=null && n.keyF(key, scancode, a)) return true;
    if (focusedVW!=null) return focusedVW.key(key, scancode, a);
    return false;
  }
  
  public void typed(int p) {
    Node n = focusNode();
    if (n!=null) n.typed(p);
  }
  
  
  private boolean requestDraw;
  public boolean requiresDraw() {
    if (requestDraw) return true;
    for (VirtualWindow c : vws) if (c.requiresRedraw()) return true;
    return false;
  }
  public boolean draw(Graphics g, boolean full) {
    float shBlur = gc.getProp("menu.shadowBlur").lenF();
    float shSpread = gc.getProp("menu.shadowSpread").lenF();
    int shColor = gc.getProp("menu.shadowColor").col();
    boolean any = requestDraw;
    for (VirtualWindow c : vws) {
      if (c.shouldRemove()) continue;
      if (c.draw()) any = true;
      c.drawTo(g);
      if (c.drawShadow()) g.canvas.drawRectShadow(c.rect.skiaf().inflate(1), 0, 0, shBlur, shSpread, shColor);
    }
    
    if (tools!=null) tools.drawInsp(g);
    requestDraw = false;
    return any || full;
  }
  
  
  
  public void maybeResize() {
    for (VirtualWindow vw : vws) vw.maybeResize();
  }
  private Surface lastSurface; // TODO decide if there's a better way
  public void resized(Surface s) {
    lastSurface = s;
    for (VirtualWindow vw : vws) vw.newSurface(s, w, h);
  }
  
  public void openDevtoolsTo(Node n) { // for debugging
    Devtools t = impl.createTools();
    if (t!=null) t.toOpen.set(n);
  }
  
  
  
  public void cfgUpdated() {
    recProps(base);
    base.mRedraw();
    base.mResize();
  }
  private void recProps(Node n) {
    n.mProp();
    for (Node c : n.ch) recProps(c);
  }
  
  
  public String getClip() {
    try {
      return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
    } catch (UnsupportedFlavorException | IOException e) {
      return null;
    }
  }
  public void setClip(String s) {
    StringSelection stringSelection = new StringSelection(s);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
  }
}