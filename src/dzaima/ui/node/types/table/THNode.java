package dzaima.ui.node.types.table;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.ScrollNode;
import dzaima.utils.Tools;

public class THNode extends TRTNode {
  public THNode(Ctx ctx, String[] ks, Prop[] vs) {
    super(ctx, ks, vs);
    aTick();
  }
  
  ScrollNode sticky;
  public void propsUpd() { super.propsUpd();
    if (gc.boolD(this, "sticky", false)) {
      Node c = p;
      while (!(c instanceof ScrollNode)) c = c.p;
      sticky = (ScrollNode) c;
    } else sticky = null;
  }
  
  public void tickC() {
    if (sticky!=null) dy = -sticky.ch().dy;
  }
  
  public void bg(Graphics g, boolean full) {
    int c = hasBg? bg : t.bgH;
    if (Tools.st(c)) pbg(g, full);
    if (Tools.vs(c)) g.rect(0, 0, w, h, c);
    if (t.sep>0) {
      for (Node n : ch) {
        int x = n.dx-t.padX-t.sep;
        g.rect(x, t.sepY, x+t.sep, h-t.sepY, t.sepCol);
      }
    }
  }
}
