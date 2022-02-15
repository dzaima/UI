package dzaima.ui.node.types;

import dzaima.ui.gui.Font;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public abstract class InlineNode extends Node {
  public InlineNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  public short sX, sY1, sY2;
  public short eX, eY1, eY2;
  
  public final int maxH(int w) { return minH(w); }
  
  public Font getFont() {
    return p instanceof InlineNode? ((InlineNode) p).getFont() : gc.defFont;
  }
  
  protected abstract void addInline(InlineSolver sv);
  protected abstract void baseline(int asc, int dsc);
  
  public static class InlineSolver {
    public final int w; // total max width allocated for this
    public final boolean resize;
    public int tcol, tbg;
    public Font f;
    
    public float x; // left of next character's x
    public int y; // top y of current line
    public int a, b, h; // heights of current line - above baseline, below baseline, and total (for non-inline elements)
    public Vec<InlineNode> ln = new Vec<>(); // last line items to update about baseline info
    
    public InlineSolver(int w, GConfig gc, boolean resize) {
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
        int ch = n.minH(w);
        if (x+cw > w) nl();
        if (resize) n.resize(cw, ch, (int)x, y);
        x+= cw;
        h = Math.max(h, ch);
      }
    }
    
    public void nl() {
      int ny = y + Math.max(a+b, h);
      if (resize) {
        for (InlineNode c : ln) {
          if (c.sY2==-1) c.sY2 = (short)ny;
          c.eY2 = (short)ny;
          c.baseline(a, b); // TODO pass h in too?
          c.h = ny;
        }
      }
      ln.sz0();
      y = ny;
      x = 0;
      a = b = h = 0;
    }
  
    void ab(int a, int b) {
      this.a = Math.max(this.a, a);
      this.b = Math.max(this.b, b);
    }
  }
  
  public Node findCh(int x, int y) {
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
      if (sY1==eY1) return x>=sX && x<eX;
      else return y<sY2? x>=sX : y>=eY1? x<eX : true;
    }
    return false;
  }
  
  
  public static class LineEnd extends InlineNode {
    public LineEnd(Ctx ctx) { super(ctx, KS_NONE, VS_NONE); }
    protected void addInline(InlineSolver sv) { sv.x = sv.w; }
    protected void baseline(int asc, int dsc) { }
  }
  
  public static class FullBlock extends InlineNode {
    public FullBlock(Ctx ctx) { super(ctx, KS_NONE, VS_NONE); }
    protected void addInline(InlineSolver sv) {
      if (sv.x!=0) sv.nl();
      Node c = ch.get(0);
      c.resize(sv.w, c.minH(sv.w), (int)sv.x, sv.y);
      sv.a = 0; sv.b = 0;
      sv.y+= c.h;
    }
    protected void baseline(int asc, int dsc) { }
    public static FullBlock wrap(Node n) {
      FullBlock b = new FullBlock(n.ctx);
      b.add(n);
      return b;
    }
  }
  
  public static abstract class SubSelConsumer {
    public abstract void addString(StringNode nd, String s);
    public abstract void addNode(Node nd);
  }
  
  private static void selFull(Node n, SubSelConsumer ssc) {
    if (n instanceof StringNode) ssc.addString((StringNode) n, ((StringNode) n).s);
    if (n instanceof InlineNode) for (Node c : n.ch) selFull(c, ssc);
    else ssc.addNode(n);
  }
  private static void selLeft(Node p, Vec<Node> ps, Position.Spec s, SubSelConsumer ssc) {
    if (ps.sz==0) {
      StringNode sn = (StringNode) p;
      if (s.pos==-1) { ssc.addString(sn, "??"); return; } // TODO remove
      ssc.addString(sn, sn.s.substring(s.pos));
    } else {
      Node n = ps.pop();
      selLeft(n, ps, s, ssc);
      for (int i = p.ch.indexOf(n)+1; i < p.ch.sz; i++) selFull(p.ch.get(i), ssc);
    }
  }
  private static void selRight(Node p, Vec<Node> ps, Position.Spec s, SubSelConsumer ssc) {
    if (ps.sz==0) {
      StringNode sn = (StringNode) p;
      if (s.pos==-1) { ssc.addString(sn, "??"); return; } // TODO remove
      ssc.addString(sn, sn.s.substring(0, s.pos));
    } else {
      Node n = ps.pop();
      for (int i = 0, e=p.ch.indexOf(n); i < e; i++) selFull(p.ch.get(i), ssc);
      selRight(n, ps, s, ssc);
    }
  }
  public static void parseSelection(Selection s, SubSelConsumer ssc) {
    Node n = s.c;
    
    Node aC = s.aS.ln; Vec<Node> aP = new Vec<>(); while (aC!=n) aC = aP.add(aC).p;
    Node bC = s.bS.ln; Vec<Node> bP = new Vec<>(); while (bC!=n) bC = bP.add(bC).p;
  
    while (true) {
      if (aP.sz==0 || bP.sz==0) {
        assert n instanceof StringNode;
        int aN = s.aS.pos;
        int bN = s.bS.pos;
        if (aN==-1||bN==-1) return; // TODO remove
        ssc.addString((StringNode) n, ((StringNode) n).s.substring(Math.min(aN, bN), Math.max(aN, bN)));
        return;
      }
      Node aT = aP.pop();
      Node bT = bP.pop();
      if (aT == bT) {
        n = aT;
        continue;
      }
      int aI = n.ch.indexOf(aT);
      int bI = n.ch.indexOf(bT);
      boolean as = aI<bI; int sI = as?aI:bI; Node sT = as?aT:bT; Vec<Node> sP = as?aP:bP; Position.Spec sS = as?s.aS:s.bS;
      boolean ae = aI>bI; int eI = ae?aI:bI; Node eT = ae?aT:bT; Vec<Node> eP = ae?aP:bP; Position.Spec eS = ae?s.aS:s.bS;
      
      selLeft(sT, sP, sS, ssc);
      for (int i = sI+1; i < eI; i++) selFull(n.ch.get(i), ssc);
      selRight(eT, eP, eS, ssc);
      
      return;
    }
  }
  public static String getSelection(Selection s) {
    StringBuilder res = new StringBuilder();
    parseSelection(s, new SubSelConsumer() {
      public void addString(StringNode nd, String s) { res.append(s); }
      public void addNode(Node nd) { }
    });
    return res.toString();
  }
}
