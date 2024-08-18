package dzaima.ui.node.types;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.Collections;

public class PadNode extends Node implements InlineNode.Scannable {
  
  private int pL, pU, pX, pY, bgCol;
  public PadNode(Ctx ctx, Props props) {
    super(ctx, props);
    if (hasProp("bgCol")) Log.warn("ui", "Using incorrect 'bgCol' property");
  }
  public void propsUpd() { super.propsUpd();
    bgCol = gc.colD(this, "bg", 0);
    int all = gc.pxD(this, "all", 0);
    int l = gc.pxD(this, "l", 0);
    int r = gc.pxD(this, "r", 0);
    int u = gc.pxD(this, "u", 0);
    int d = gc.pxD(this, "d", 0);
    int x = gc.pxD(this, "x", 0);
    int y = gc.pxD(this, "y", 0);
    pL = all+x+l   ; pU = all+y+u   ;
    pX = all+x+r+pL; pY = all+y+d+pU;
  }
  
  public void bg(Graphics g, boolean full) {
    if (Tools.st(bgCol)) pbg(g, full);
    if (Tools.vs(bgCol)) g.rect(0, 0, w, h, bgCol);
  }
  
  public Node ch() {
    return ch.get(0);
  }
  
  public int minW() { return ch().minW()+pX; }
  public int maxW() { return ch().maxW()+pX; }
  public int minH(int w) { return ch().minH(w-pX)+pY; }
  public int maxH(int w) { return ch().maxH(w-pX)+pY; }
  
  public void resized() {
    assert ch.sz==1 : Devtools.debugMe(this)+" should have exactly 1 child";
    ch().resize(w-pX, h-pY, pL, pU);
  }
  
  public Iterable<Node> scannableCh() {
    Prop sc = getPropN("scannable");
    return sc==null || sc.b()? ch : Collections.emptyList();
  }
}
