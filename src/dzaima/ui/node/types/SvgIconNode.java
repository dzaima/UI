package dzaima.ui.node.types;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;
import io.github.humbleui.skija.*;

public class SvgIconNode extends Node {
  private String src;
  private Path p;
  private Paint col;
  private int w, h;
  private float sc;
  public SvgIconNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
  }
  
  public void propsUpd() { super.propsUpd();
    String nsrc = vs[id("src")].str();
    if (!nsrc.equals(src)) {
      src = nsrc;
      p = Path.makeFromSVGString(src);
    }
    int iwi = id("iw");
    int ihi = id("ih");
    if (iwi==-1 && ihi==-1) {
      Log.warn("node 'svgicon'", "Using old parameters");
      sc = vs[id("sz")].f()*gc.em*(1f/169);
      w = Tools.ceil(vs[id("w")].f()*sc);
      h = Tools.ceil(vs[id("h")].f()*sc);
    } else {
      int iw = vs[iwi].i();
      int ih = vs[ihi].i();
      int rwi = id("w");
      int rhi = id("h");
      assert (rwi==-1) != (rhi==-1) : Devtools.debugMe(this)+": svgicon must have exactly one of 'w' and 'h'";
      if (rwi!=-1) { w=vs[rwi].len(); sc=w*1f/iw; h=Tools.ceil(sc*ih); }
      else         { h=vs[rhi].len(); sc=h*1f/ih; w=Tools.ceil(sc*iw); }
    }
    col = new Paint().setColor(gc.col(this, "color", "icon.color"));
  }
  
  public void drawC(Graphics g) {
    g.push();
    g.canvas.scale(sc, sc);
    g.canvas.drawPath(p, col);
    g.pop();
  }
  
  public int minW(     ) { return w; }
  public int maxW(     ) { return w; }
  public int minH(int w) { return h; }
  public int maxH(int w) { return h; }
}
