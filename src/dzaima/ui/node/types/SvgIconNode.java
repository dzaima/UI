package dzaima.ui.node.types;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.Tools;
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
    sc = vs[id("sz")].f()*gc.em*(1f/169);
    w = Tools.ceil(vs[id("w")].f()*sc);
    h = Tools.ceil(vs[id("h")].f()*sc);
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
