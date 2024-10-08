package dzaima.ui.node.types;

import dzaima.ui.gui.Font;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.function.Consumer;

public abstract class InlineNode extends Node {
  public InlineNode(Ctx ctx, Props props) {
    super(ctx, props);
  }
  public short sX, sY1, sY2; // first line coords
  public short eX, eY1, eY2; // last line coords; TODO could use h for eY2?
  public boolean isSingleLine() { return sY1==eY1 && sY2==eY2; }
  
  public final int maxH(int w) { return minH(w); }
  
  public Font getFont() {
    return p instanceof InlineNode? ((InlineNode) p).getFont() : gc.defFont;
  }
  
  protected abstract void addInline(InlineSolver sv);
  protected abstract void baseline(int asc, int dsc, int h);
  protected boolean inlineChildren() { return true; }
  
  public static class InlineSolver {
    public final InlineNode resized; // node being resized, and thus whose parent should be ignored
    public final int w; // total max width allocated for this
    public final boolean resize;
    public int tcol, tbg;
    public Font f;
    
    public float x; // left of next character's x
    public int y; // top y of current line
    public int a, b, h; // heights of current line - above baseline, below baseline, and total (for non-inline elements)
    public Vec<InlineNode> ln = new Vec<>(); // last line items to update about baseline info
    
    public InlineSolver(InlineNode resized, int w, GConfig gc, boolean resize) {
      this.resized = resized;
      this.w = w;
      this.f = gc.defFont;
      this.tcol = gc.cfg.get("str.color").col();
      this.tbg = 0;
      this.resize = resize;
    }
    
    public void add(Node n) {
      if (n instanceof InlineNode) {
        InlineNode c = (InlineNode) n;
        ln.add(c);
        if (resize) {
          c.w = w; // h written on nl
          c.sX = (short) x;
          c.sY1 = (short) y;
          c.sY2 = -1;
          c.eY2 = -1;
        }
        c.addInline(this);
        if (resize) {
          c.eX = (short) x;
          c.eY1 = (short) y;
        }
        ln.add(c);
      } else {
        int cw = n.minW();
        int ch = n.minH(cw);
        if (x+cw > w) nl();
        int x0 = Tools.ceil(x);
        if (resize) n.resize(cw, ch, x0, y);
        x = x0+cw;
        h = Math.max(h, ch);
      }
    }
    
    public void nl() {
      nl(0, 0);
    }
    public void nl(int na, int nb) {
      int ny = y + Math.max(a+b, h);
      if (resize) {
        for (InlineNode c : ln) {
          boolean start = c.sY2==-1;
          if (start) c.sY2 = (short)ny;
          c.eY2 = (short)ny;
          if (!start) c.baseline(a, b, h);
          c.h = ny;
        }
      }
      ln.sz0();
      y = ny;
      x = 0;
      h = 0;
      a = na;
      b = nb;
    }
    
    void ab(int a, int b) {
      this.a = Math.max(this.a, a);
      this.b = Math.max(this.b, b);
    }
  }
  
  public Node nearestCh(int x, int y) {
    if (!inlineChildren()) return super.nearestCh(x, y);
    Node found = findCh(x, y); // working off the assumption that this node is fully covered by the children
    if (found!=null || ch.sz==0) return found;
    return y>h || y>0 && x>0? ch.peek() : ch.get(0);
  }
  public Node findCh(int x, int y) {
    if (!inlineChildren()) return super.findCh(x, y);
    for (Node c : ch) {
      if (c instanceof InlineNode) {
        if (((InlineNode) c).in(x, y)) return c;
      } else {
        if (XY.inWH(x, y, c.dx, c.dy, c.w, c.h)) return c;
      }
    }
    return null;
  }
  public boolean in(int x, int y) {
    if (y>=sY1 && y<eY2) {
      if (isSingleLine()) return x>=sX && x<eX;
      else return y<sY2? x>=sX : y>=eY1? x<eX : true;
    }
    return false;
  }
  
  
  public static class LineEnd extends InlineNode implements StringifiableNode {
    private final boolean returnNewline;
    public LineEnd(Ctx ctx, boolean returnNewline) { super(ctx, Props.none()); this.returnNewline = returnNewline; }
    protected void addInline(InlineSolver sv) { sv.x = sv.w; }
    protected void baseline(int asc, int dsc, int h) { }
    public String asString() {
      return returnNewline? "\n" : "";
    }
  }
  
  public static class FullBlock extends InlineNode {
    public FullBlock(Node ch) { super(ch.ctx, Props.none()); add(ch); }
    protected void addInline(InlineSolver sv) {
      if (sv.x!=0) sv.nl();
      
      Node c = ch.get(0);
      int ch = c.minH(sv.w);
      if (sv.resize) c.resize(sv.w, ch, 0, sv.y);
      
      sv.y+= ch;
      sv.h = sv.a = sv.b = 0;
      sv.x = sv.w;
    }
    protected void baseline(int asc, int dsc, int h) { }
    public int minW(     ) { return ch.get(0).minW( ); }
    public int minH(int w) { return ch.get(0).minH(w); }
    
    protected boolean inlineChildren() { return false; }
  }
  public static class TANode extends InlineNode {
    public TANode(Ctx ctx, Props props) {
      super(ctx, props);
    }
    
    protected String mode() { Prop m = getPropN("mode"); return m==null? "top" : m.val(); }
    protected void baseline(int asc, int dsc, int h) {
      Node n = ch.get(0);
      switch (mode()) {
        case "above": n.dy+= asc-n.h; break;
        case "below": n.dy+= asc; break;
        case "top": return;
        case "bottom": if (n.h<h) n.dy+= h-n.h; break;
      }
    }
    protected void addInline(InlineSolver sv) {
      Node n = ch.get(0);
      boolean maxW = false;
      
      Prop width = getPropN("width");
      if (width!=null) switch (width.val()) { default: Log.error("node 'ta'", "width should be either min or max"); break;
        case "min": maxW = false; break;
        case "max": maxW = true; break;
      }
      
      int cw = maxW? Math.min(sv.w, n.maxW()) : n.minW();
      int ch = n.minH(cw);
      if (sv.x+cw > sv.w) sv.nl();
      int x0 = Tools.ceil(sv.x);
      if (sv.resize) n.resize(cw, ch, x0, sv.y);
      sv.x = x0+cw;
      switch (mode()) {
        case "top":
        case "bottom": sv.h = Math.max(sv.h, ch); break;
        case "above":  sv.a = Math.max(sv.a, ch); break;
        case "below":  sv.b = Math.max(sv.b, ch); break;
        default: Log.warn("node 'ta'", "invalid mode \""+mode()+"\""); break;
      }
    }
    public int minW() { return ch.get(0).minW(); }
    public int maxW() { return ch.get(0).maxW(); }
    public int minH(int w) { return ch.get(0).minH(w); }
  }
  
  public static abstract class SubSelConsumer {
    public abstract void addString(StringNode nd, int s, int e);
    public abstract void addNode(Node nd);
  }
  
  public interface Scannable {
    Iterable<Node> scannableCh();
  }
  
  public static void scanNode(Node n, SubSelConsumer ssc) {
    if (n instanceof StringNode) ssc.addString((StringNode) n, 0, ((StringNode) n).s.length());
    ssc.addNode(n);
    if (n instanceof Scannable) for (Node c : ((Scannable) n).scannableCh()) scanNode(c, ssc);
    else if (n instanceof InlineNode) for (Node c : n.ch) scanNode(c, ssc);
  }
  public static boolean scanSelection(Selection s, SubSelConsumer ssc) { // returns if aS>bS
    Node gp = s.cNode();
    
    Node aC = s.aS.ln; Vec<Node> aP = new Vec<>(); while (aC!=gp) aC = aP.add(aC).p;
    Node bC = s.bS.ln; Vec<Node> bP = new Vec<>(); while (bC!=gp) bC = bP.add(bC).p;
    
    while (true) {
      if (aP.sz==0 || bP.sz==0) {
        int aN = s.aS.pos;
        int bN = s.bS.pos;
        if (gp instanceof StringNode) ssc.addString((StringNode) gp, Math.min(aN, bN), Math.max(aN, bN));
        else if (aN!=bN) ssc.addNode(gp);
        return aN>bN;
      }
      Node aT = aP.peek();
      Node bT = bP.peek();
      if (aT == bT) {
        gp = aT;
        aP.pop();
        bP.pop();
        continue;
      }
      int aI = gp.ch.indexOf(aT);
      int bI = gp.ch.indexOf(bT);
      boolean as = aI<bI; int sI = as?aI:bI; Vec<Node> sP = as?aP:bP; PosPart sS = as?s.aS:s.bS;
      boolean ae = aI>bI; int eI = ae?aI:bI; Vec<Node> eP = ae?aP:bP; PosPart eS = ae?s.aS:s.bS;
      
      // starting substring
      if (sS.ln instanceof StringNode) ssc.addString((StringNode) sS.ln, sS.pos, ((StringNode) sS.ln).s.length());
      else if (sS.pos==0) ssc.addNode(sS.ln);
      // climb up to common node
      for (int i = 1; i < sP.sz; i++) {
        Node p=sP.get(i), c=sP.get(i-1);
        for (int j = p.ch.indexOf(c)+1; j < p.ch.sz; j++) scanNode(p.ch.get(j), ssc);
      }
      // all basic nodes in the middle
      for (int i = sI+1; i < eI; i++) scanNode(gp.ch.get(i), ssc);
      // climb down to ending
      for (int i = eP.sz-1; i >= 1; i--) {
        Node p=eP.get(i), c=eP.get(i-1);
        for (int j = 0, e=p.ch.indexOf(c); j < e; j++) scanNode(p.ch.get(j), ssc);
      }
      // ending substring
      if (eS.ln instanceof StringNode) ssc.addString((StringNode) eS.ln, 0, eS.pos);
      else if (eS.pos==1) ssc.addNode(eS.ln);
      
      return ae;
    }
  }
  
  public static String sscText(Consumer<SubSelConsumer> f) {
    StringBuilder res = new StringBuilder();
    f.accept(new SubSelConsumer() {
      public void addString(StringNode nd, int s, int e) { res.append(nd.s, s, e); }
      public void addNode(Node nd) { if (nd instanceof StringifiableNode) res.append(((StringifiableNode) nd).asString()); }
    });
    return res.toString();
  }
  
  public static String getSelection(Selection s) {
    return sscText(ssc -> scanSelection(s, ssc));
  }
  public static String getNodeText(Node n) {
    return sscText(ssc -> scanNode(n, ssc));
  }
}
