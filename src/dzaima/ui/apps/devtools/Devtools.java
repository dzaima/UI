package dzaima.ui.apps.devtools;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.*;
import dzaima.utils.*;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

public class Devtools extends NodeWindow {
  public static int openDevtools = 0;
  
  public final NodeWindow insp;
  public boolean pick;
  public boolean hlInline = true;
  DTGraphNode graph;
  
  public Devtools(GConfig gc, Ctx pctx, PNodeGroup g, NodeWindow insp, Rect r) {
    super(gc, pctx, g, new WindowInit("Devtools", r));
    graph = (DTGraphNode) base.ctx.id("graph");
    graph.t = this;
    openDevtools++;
    this.insp = insp;
  }
  
  public static Devtools create(NodeWindow w) {
    GConfig gc = GConfig.newConfig();
    BaseCtx ctx = Ctx.newCtx();
    ctx.put("dtgraph", DTGraphNode::new);
    Devtools dt = new Devtools(gc, ctx, gc.getProp("devtools.ui").gr(), w, Windows.defaultWindowRect());
    w.tools = dt;
    return dt;
  }
  
  public static Devtools getDevtools(Node n) {
    if (openDevtools==0) return null;
    return n.ctx.win().tools;
  }
  
  public static String name(Node insp, boolean ml) {
    String text;
    if (insp instanceof StringNode) {
      String s = ((StringNode) insp).s;
      if (!ml) {
        int m = s.indexOf('\r');
        if (m==-1) m = s.indexOf('\n');
        if (m!=-1) s = s.substring(0, m)+"…";
      }
      return "\""+s+"\"";
    }
    
    text = insp.getClass().getSimpleName();
    if (text.equals("")) {
      Class<?> c = insp.getClass();
      while (c.getSimpleName().equals("")) c = c.getSuperclass();
      text = c.getSimpleName();
      
      Class<?> ec = insp.getClass().getEnclosingClass();
      if (ec!=null) text = ec.getSimpleName()+"→"+text;
    } else {
      if (text.endsWith("Node")) text = text.substring(0, text.length()-4);
      int lu = 0;
      while (lu < text.length() && Character.isUpperCase(text.charAt(lu))) lu++;
      text = text.substring(0,lu).toLowerCase()+text.substring(lu);
    }
    int idID = insp.id("id");
    if (idID!=-1) text+= "#"+insp.vs[idID];
    return text;
  }
  
  public void setup() { super.setup();
    Node tree = base.ctx.id("main");
    tree.add(new DTTNNode(this, base.ctx, insp.base));
    ((BtnNode) base.ctx.id("pick")).setFn(b -> pick = true);
    ((BtnNode) base.ctx.id("hlInline")).setFn(b -> { hlInline^= true; newSel.set(true); });
    ((BtnNode) base.ctx.id("dbgRedraw")).setFn(b -> { WindowImpl.DEBUG_REDRAW^= true; });
    focus(tree);
    String t = insp.getTitle();
    base.ctx.id("infoL").add(new StringNode(base.ctx, t==null? "(null title)" : t));
  }
  public void stopped() { super.stopped();
    Node base = insp.base;
    if (base!=null) base.mRedraw();
    insp.tools = null;
    openDevtools--;
  }
  
  
  ConcurrentLinkedQueue<Node> modified = new ConcurrentLinkedQueue<>();
  public AtomicReference<Node> toOpen = new AtomicReference<>(null);
  String lastError;
  Node selected; // item whose properties are shown
  Node highlight; // actually highlighted node
  final AtomicBoolean newSel = new AtomicBoolean(true);
  public void tick() { super.tick();
    if (frameCount%5==0) refresh();
    Node cOpen = toOpen.getAndSet(null);
    if (cOpen!=null) {
      impl.focus();
      focus(find(cOpen, true));
      pick = false;
    }
  
    Node nHL = null;
    if (pick) {
      nHL = find(insp.mx, insp.my);
    } else if (focusNode instanceof DTTNNode) {
      nHL = ((DTTNNode) focusNode).insp;
      if (!nHL.visible) nHL = null;
    }
    Node nSel = nHL==null? selected : nHL;
    if (highlight!=nHL || selected!=nSel) {
      newSel.set(true);
      Node path = base.ctx.id("path"); path.clearCh();
      if (nSel!=null && nSel.visible) {
        Vec<Node> v = new Vec<>();
        Node c = nSel;
        while (c!=null) { v.add(c); c = c.p; }
        StringBuilder b = new StringBuilder();
        for (int i = v.sz-1; i >= 0; i--) b.append(name(v.get(i), false)).append(i==0? "" : " → ");
        path.add(new StringNode(base.ctx, b.toString()));
        ((ScrollNode) base.ctx.id("pathScroll")).toRight();
      } else path.add(new StringNode(base.ctx, ""));
    }
    highlight = nHL;
    selected = nSel;
    
    boolean any = false;
    while (true) {
      Node c = modified.poll(); if (c==null) break;
      DTTNNode wr = find(c, false);
      if (wr.insp!=c) continue;
      wr.dtUpd();
      any = true;
    }
    if (any) refresh();
    
    // graph.mRedraw();
  }
  
  
  // important: these must only atomically mutate devtools
  public void drawInsp(Graphics g) {
    if (highlight!=null) {
      XY p = screenPos(highlight);
      int hlCol1 = 0x7f214283;
      int hlCol2 = 0x7f215273;
      g.translate(p.x, p.y);
      if (hlInline && highlight instanceof InlineNode && ((InlineNode) highlight).eY2!=0) {
        InlineNode c = (InlineNode) highlight;
        if (c.sY1==c.eY1) {
          g.rect(c.sX, c.sY1, c.eX, c.eY2, hlCol1);
        } else {
          g.rect(c.sX, c.sY1, c.w, c.sY2, hlCol2);
          g.rect(0, c.sY2, c.w, c.eY1, hlCol1);
          g.rect(0, c.eY1, c.eX, c.eY2, hlCol2);
        }
      } else {
        g.rect(0, 0, highlight.w, highlight.h, hlCol1);
      }
      g.translate(-p.x, -p.y);
    }
  }
  public boolean mouseDownInsp(int x, int y, Click cl) {
    if (!pick || cl.btn!=Click.LEFT) return false;
    
    toOpen.set(find(x, y));
    return true;
  }
  public int redrawInsp() { // 0-nothing needed; 1-if redrawing, redraw all; 2-request redraw
    if (newSel.get()) {
      newSel.set(false);
      return 2;
    }
    return pick? 2 : highlight!=null? 1 : 0;
  }
  public void stoppedInsp() {
    closeOnNext();
  }
  public void modified(Node n) {
    modified.add(n); // TODO maybe read updated children here, instead of during devtools tick?
  }
  public void errorReport(Throwable t) {
    StringWriter s = new StringWriter();
    t.printStackTrace(new PrintWriter(s));
    lastError = s.toString();
  }
  
  
  
  
  public void focus(Node n) { super.focus(n);
    if (n instanceof DTTNNode) refresh();
  }
  public void refresh() {
    Node iR = base.ctx.id("infoR");
    iR.clearCh();
    iR.add(new StringNode(iR.ctx, String.format("%s %.2f FPS", Windows.getManager(), 1e9/getTime("frame"))));
  
    Node iC = base.ctx.id("infoC");
    iC.clearCh();
    iC.add(new StringNode(iC.ctx, String.format("event %6.3fms", getTime("event")/1e6)));
    iC.add(new StringNode(iC.ctx, String.format(" tick %6.3fms", getTime("tick" )/1e6)));
    iC.add(new StringNode(iC.ctx, String.format(" draw %6.3fms", getTime("draw" )/1e6)));
    iC.add(new StringNode(iC.ctx, String.format("flush %6.3fms", getTime("flush")/1e6)));
    iC.add(new StringNode(iC.ctx, String.format(" wait %6.3fms", (getTime("frame")-getTime("all"))/1e6)));
    if (lastError!=null) iC.add(new StringNode(iC.ctx, lastError));
    
    
    Node infoT = base.ctx.id("infoT"); infoT.clearCh();
    if (selected!=null && selected.visible) {
      Node insp = selected;
      XY pos = screenPos(insp);
      
      // ks.add(new StringNode(base.ctx, "self:visible"  )); vs.add(new StringNode(base.ctx, focusNode.visible+""));
      // ks.add(new StringNode(base.ctx, "self:open"     )); vs.add(new StringNode(base.ctx, ((DTTNNode) focusNode).open+""));
      // ks.add(new StringNode(base.ctx, "self:openable" )); vs.add(new StringNode(base.ctx, ((DTTNNode) focusNode).openable+""));
      // ks.add(new StringNode(base.ctx, "self:ch.sz"    )); vs.add(new StringNode(base.ctx, focusNode.ch.sz+""));
      
      // addRow(infoT, "flag:mtick"    , Boolean.toString((insp.flags&Node.MTICK)!=0));
      // addRow(infoT, "flag:atick"    , Boolean.toString((insp.flags&Node.ATICK)!=0));
      // addRow(infoT, "flag:props"    , Boolean.toString((insp.flags&Node.PROPS)!=0));
      // addRow(infoT, "flag:visible"  , Boolean.toString(insp.visible));
      // addRow(infoT, "field:ctx"     , insp.ctx+"");
      addRow(infoT, "actual width"  , insp.w+"px");
      addRow(infoT, "actual height" , insp.h+"px");
      addRow(infoT, "position X"    , pos.x+"px");
      addRow(infoT, "position Y"    , pos.y+"px");
      addRow(infoT, "delta X"       , insp.dx+"px");
      addRow(infoT, "delta Y"       , insp.dy+"px");
      addRow(infoT, "children count", insp.ch.sz+"");
      if (selected instanceof InlineNode) {
        InlineNode c = (InlineNode) selected;
        addRow(infoT, "first line X/Y", "x="+c.sX+" y0="+c.sY1+" y1="+c.sY2);
        addRow(infoT, "last line X/Y", "x="+c.eX+" y0="+c.eY1+" y1="+c.eY2);
        if (c instanceof StringNode && ((StringNode) c).words!=null) {
          for (StringNode.Word w : ((StringNode) c).words) {
            addRow(infoT, "\""+w.s+"\"", "x="+w.x+" y="+w.y+" w="+w.w+" fl="+w.flags+" spl="+(w.split==null? "ø" : w.split.length));
          }
        }
      }
      
      for (int i = 0; i < insp.ks.length; i++) {
        addRow(infoT, insp.ks[i], insp.vs[i].toString());
      }
    }
  }
  
  private void addRow(Node table, String k, String v) {
    Node r = table.ctx.make(row);
    r.ctx.id("k").add(new StringNode(table.ctx, k));
    r.ctx.id("v").add(new StringNode(table.ctx, v));
    table.add(r);
  }
  PNodeGroup row = Prs.parseNode(
    "tr {" +
    "  pad { id=k l=.5em y=.1em family=\"DejaVu Sans Mono\" }" +
    "  pad { id=v r=.5em y=.1em family=\"DejaVu Sans Mono\" }" +
    "}");
  
  
  
  private XY screenPos(Node c) {
    return c.relPos(null);
  }
  public DTTNNode find(Node n, boolean create) {
    Vec<Node> path = new Vec<>();
    while (n!=null) {
      path.add(n);
      n = n.p;
    }
    DTTNNode c = (DTTNNode) base.ctx.id("main").ch.get(0);
    for (int i = path.sz-2; i >= 0; i--) {
      Node pathC = path.get(i);
      if (!c.open) {
        if (!create) return c;
        c.open();
      }
      for (Node a : c.ch) {
        if (a instanceof DTTNNode && ((DTTNNode) a).insp==pathC) {
          c = (DTTNNode) a;
          break;
        }
      }
    }
    return c;
  }
  public Node find(int x, int y) {
    Node c = insp.base;
    while (true) {
      x-= c.dx;
      y-= c.dy;
      Node n = c.findCh(x, y);
      if (n==null) break;
      c = n;
    }
    return c;
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (super.key(key, scancode, a)) return true;
    if (a.press && key.k_esc()) { closeOnNext(); return true; }
    return false;
  }
  
  public long getTime(String s) {
    RotBuf l = times.get(s);
    if (l==null) return 0;
    return l.sum / RotBuf.SUM_LEN;
  }
  
  long prevFrameTime = -1;
  long prevTime = -1;
  public void timeStart(long sns) {
    if (prevFrameTime==-1) prevFrameTime = sns;
    prevFrameTime = time("frame", prevFrameTime);
    prevTime = sns;
  }
  
  HashMap<String, RotBuf> times = new HashMap<>();
  public void time(String name) {
    prevTime = time(name, prevTime);
  }
  public long time(String name, long sns) {
    long ens = System.nanoTime();
    timeDirect(name, ens-sns);
    return ens;
  }
  public void timeDirect(String name, long ns) {
    RotBuf b = times.get(name);
    if (b==null) times.put(name, b = new RotBuf());
    b.add(ns);
  }
}
