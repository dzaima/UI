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
    String nsrc = getProp("src").str();
    if (!nsrc.equals(src)) {
      src = nsrc;
      p = Path.makeFromSVGString(src);
    }
    int iw = getProp("iw").i();
    int ih = getProp("ih").i();
    Prop rw = getPropN("w");
    Prop rh = getPropN("h");
    assert (rw==null) != (rh==null) : Devtools.debugMe(this)+": svgicon must have exactly one of 'w' and 'h'";
    if (rw!=null) { w=rw.len(); sc=w*1f/iw; h=Tools.ceil(sc*ih); }
    else          { h=rh.len(); sc=h*1f/ih; w=Tools.ceil(sc*iw); }
    if (col!=null) col.close();
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
