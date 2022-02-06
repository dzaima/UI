package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;

public abstract class FrameNode extends Node {
  protected byte W_PR, H_PR, XA_PR, YA_PR;
  public FrameNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    W_PR = byteID("w");
    H_PR = byteID("h");
    if (W_PR==-1) { W_PR = byteID("width"); if(W_PR!=-1) System.err.println("warning: using old width property"); }
    if (H_PR==-1) { H_PR = byteID("height"); if(H_PR!=-1) System.err.println("warning: using old height property"); }
    XA_PR = byteID("alX");
    YA_PR = byteID("alY");
    if (XA_PR==-1) { XA_PR = byteID("xalign"); if(XA_PR!=-1) System.err.println("warning: using old xalign property"); }
    if (YA_PR==-1) { YA_PR = byteID("yalign"); if(YA_PR!=-1) System.err.println("warning: using old yalign property"); }
  }
  byte byteID(String s) {
    int id = id(s);
    if (id != (byte) id) throw new RuntimeException("Too many properties!");
    return (byte) id;
  }
  
  public void bg(Graphics g, boolean full) {
    int p = id("bg");
    if (p==-1) { p=id("bgCol"); if(p!=-1)System.err.println("warning: using old bgCol property"); }
    int col = p<0? 0 : vs[p].col();
    if (Tools.st(col)) pbg(g, full);
    if (Tools.vs(col)) g.rect(0, 0, w, h, col);
  }
  
  public abstract int fillW();
  public abstract int fillH(int w);
  
  protected int fillWC=-1;
  protected int fillWC() {
    if (fillWC!=-1) return fillWC;
    return fillWC = fillW();
  }
  private int fillH=-1, fillHArg;
  protected int fillHC(int w) {
    if (fillH!=-1 & w==fillHArg) return fillH;
    return fillH = fillH(fillHArg = w);
  }
  public void mChResize() { super.mChResize();
    fillWC=fillH=-1;
  }
  
  public final int minW(     ) { return Math.max(fillWC( ), min(W_PR)); }
  public final int minH(int w) { return Math.max(fillHC(w), min(H_PR)); }
  public final int maxW() {
    int pr = max(W_PR, true);
    return pr>=Tools.BIG? pr : Math.max(fillWC(), pr);
  }
  public final int maxH(int w) {
    int pr = max(H_PR, false);
    return pr>=Tools.BIG? pr : Math.max(fillHC(w), pr);
  }
  
  int min(int id) { // minimum required size, always respected
    if (id < 0) return 0;
    Prop p = vs[id];
    char t = p.type();
    if (t=='a') return 0;
    if (t=='l') return p.len();
    if (t==':') return p.range().lenS();
    throw new RuntimeException("Bad "+ks[id]+" value of "+p);
  }
  int defMax(boolean w) { return Tools.BIG; }
  int max(int id, boolean w) { // maximum size, will be ignored if content is larger than this
    if (id < 0) return defMax(w);
    Prop p = vs[id];
    char t = p.type();
    if (t=='a') {
      String s = p.val();
      return "min".equals(s)? 0 : "max".equals(s)? Tools.BIG : Tools.err();
    }
    if (t=='l') return p.len();
    if (t==':') return p.range().lenE();
    throw new RuntimeException("Bad "+ks[id]+" value of "+p);
  }
  
  public int xalign() {
    if (XA_PR<0) return -1;
    String s = vs[XA_PR].val();
    if (s.equals("left")) return -1;
    if (s.equals("right")) return 1;
    assert s.equals("center");
    return 0;
  }
  public int yalign() {
    if (YA_PR<0) return -1;
    String s = vs[YA_PR].val();
    if (s.equals("top")) return -1;
    if (s.equals("bottom")) return 1;
    assert s.equals("center");
    return 0;
  }
}
