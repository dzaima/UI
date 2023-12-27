package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

public abstract class FrameNode extends Node {
  public FrameNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    if (hasProp("width")) Log.warn("Using incorrect 'width' property");
    if (hasProp("height")) Log.warn("Using incorrect 'height' property");
    if (hasProp("xalign")) Log.warn("Using incorrect 'xalign' property");
    if (hasProp("yalign")) Log.warn("Using incorrect 'yalign' property");
    if (hasProp("bgCol")) Log.warn("Using incorrect 'bgCol' property");
  }
  public void bg(Graphics g, boolean full) {
    Prop p = getPropN("bg");
    int col = p==null? 0 : p.col();
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
  
  public final int minW(     ) { return Math.max(fillWC( ), min(getPropN("w"))); }
  public final int minH(int w) { return Math.max(fillHC(w), min(getPropN("h"))); }
  public final int maxW() {
    int pr = max(getPropN("w"), true);
    return pr>=Tools.BIG? pr : Math.max(fillWC(), pr);
  }
  public final int maxH(int w) {
    int pr = max(getPropN("h"), false);
    return pr>=Tools.BIG? pr : Math.max(fillHC(w), pr);
  }
  
  int min(Prop p) { // minimum required size, always respected
    if (p == null) return 0;
    char t = p.type();
    if (t=='a') return 0;
    if (t=='l') return p.len();
    if (t==':') return p.range().lenS();
    throw new RuntimeException("Bad min value of "+p);
  }
  int defMax(boolean w) { return Tools.BIG; }
  int max(Prop p, boolean w) { // maximum size, will be ignored if content is larger than this
    if (p == null) return defMax(w);
    char t = p.type();
    if (t=='a') {
      String s = p.val();
      return "min".equals(s)? 0 : "max".equals(s)? Tools.BIG : Tools.err();
    }
    if (t=='l') return p.len();
    if (t==':') return p.range().lenE();
    throw new RuntimeException("Bad max value of "+p);
  }
  
  public int xalign() {
    Prop p = getPropN("alX");
    if (p == null) return -1;
    String s = p.val();
    if (s.equals("left")) return -1;
    if (s.equals("right")) return 1;
    assert s.equals("center");
    return 0;
  }
  public int yalign() {
    Prop p = getPropN("alY");
    if (p == null) return -1;
    String s = p.val();
    if (s.equals("top")) return -1;
    if (s.equals("bottom")) return 1;
    assert s.equals("center");
    return 0;
  }
  public static int align(int al, int tot, int sub) {
    return al==-1? 0 : al==1? tot-sub : (tot-sub)/2;
  }
}
