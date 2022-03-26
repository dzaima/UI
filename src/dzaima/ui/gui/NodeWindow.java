package dzaima.ui.gui;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.config.GConfig;
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
  
  public NodeWindow(Node base, WindowInit i) {
    super(i);
    this.gc = base.gc;
    this.ctx = (Ctx.WindowCtx) base.ctx;
    this.base = base;
    ctx.w = this;
    addMainVW();
  }
  public NodeWindow(GConfig gc, Ctx pctx, PNodeGroup g, WindowInit i) {
    super(i);
    this.gc = gc;
    this.ctx = new Ctx.WindowCtx(gc, pctx);
    ctx.w = this;
    this.base = ctx.makeHere(g);
    addMainVW();
  }
  
  protected void addMainVW() {
    vws.add(new VirtualWindow(this) {
      public boolean fullyOpaque() { return true; }
      public Rect getSize(int pw, int ph) {
        return new Rect(0, 0, pw, ph);
      }
      protected void implDraw(Graphics g, boolean full) {
        base.draw(g, full);
      }
      protected boolean implRequiresRedraw() {
        return base.needsRedraw();
      }
    });
  }
  
  public Node focusNode;
  public void focus(Node n) {
    Node prev = focusNode();
    focusNode = n;
    if (prev!=null) prev.focusE();
    if (n!=null && base.visible) {
      assert n.visible;
      n.focusS();
    }
  }
  public Node focusNode() {
    if (focusNode!=null && !focusNode.visible) focusNode = null;
    return focusNode;
  }
  
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
  
  public void setup() {
    base.shown();
    gc.ws.add(this); // TODO not
  }
  
  public void stopped() {
    gc.ws.remove(this);
    for (VirtualWindow c : vws) {
      c.stopped();
    }
  }
  
  public Vec<Node> pHover = new Vec<>();
  public void eventTick() {
    Node c = base;
    int cx = mx;
    int cy = my;
    Vec<Node> nHover = new Vec<>();
    int i = 0;
    while (c!=null) {
      cx-= c.dx;
      cy-= c.dy;
      nHover.add(c);
      Node p = i<pHover.sz? pHover.get(i) : null;
      if (c!=p) {
        if (p!=null) {
          for (int j = i; j < pHover.sz; j++) pHover.get(j).hoverE();
          pHover.sz0();
        }
        c.hoverS();
      }
    
      c = c.findCh(cx, cy);
      i++;
    }
    for (int j = i; j < pHover.sz; j++) pHover.get(j).hoverE();
    pHover = nHover;
  }
  
  public void tick() {
    gc.tick(impl.intentionallyLong()); // TODO this is messy
    base.tick();
  }
  
  private static boolean unusedClick;
  public void mouseDown(int x, int y, Click cl) {
    if (tools!=null && tools.mouseDownInsp(x, y, cl)) return;
    unusedClick = !base.mouseDown(x, y, cl);
  }
  public void mouseUp(int x, int y, Click cl) {
    if (unusedClick) {
      if (gc.isClick(cl)) focus(null);
    }
  }
  
  public void scroll(float dx, float dy, boolean shift) {
    if (shift) {
      dx = dy;
      dy = 0;
    }
    base.scroll(mx, my, dx, dy);
  }
  public boolean key(Key key, int scancode, KeyAction a) {
    Node n = focusNode();
    if (n!=null && n.keyF(key, scancode, a)) return true;
    return base.key(mx, my, key, scancode, a);
  }
  
  public void typed(int p) {
    Node n = focusNode();
    if (n!=null) n.typed(p);
  }
  
  
  
  public boolean requiresDraw() {
    for (VirtualWindow c : vws) if (c.requiresRedraw()) return true;
    return false;
  }
  public boolean draw(Graphics g, boolean full) {
    boolean any = false;
    for (VirtualWindow c : vws) {
      if (c.draw()) any = true;
      c.drawTo(g);
    }
    
    if (tools!=null) tools.drawInsp(g);
    return any || full;
  }
  
  
  
  public void maybeResize() {
    if (base.needsResize()) resizeCh();
  }
  public boolean windowResize;
  public void resized(Surface s) {
    for (VirtualWindow vw : vws) vw.parentResized(s, w, h);
    windowResize = true;
    resizeCh();
    windowResize = false;
  }
  public void resizeCh() {
    long sns = System.nanoTime();
    int w = Math.max(base.minW( ), Math.min(this.w,  base.maxW( )));
    int h = Math.max(base.minH(w), Math.min(this.h, base.maxH(w)));
    base.resize(w, h, 0, 0);
    long ens = System.nanoTime();
    if (PRINT_ON_RESIZE && !(this instanceof Devtools)) System.out.println((ens-sns)/1e6d+"ms resize to "+this.w+";"+this.h);
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