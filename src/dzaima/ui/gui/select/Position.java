package dzaima.ui.gui.select;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.StringNode;
import dzaima.utils.Vec;

public class Position {
  public final Node n;
  public final Vec<Spec> ss;
  protected Position(Node n, Vec<Spec> ss) {
    this.n = n;
    this.ss = ss;
  }
  
  
  
  public static class Spec {
    public final int depth;
    public final Node sn;
    public int pos; // lazy way of initializing
    public StringNode.Word word;
    
    public Spec(int depth, Node sn, int pos) {
      this.depth = depth;
      this.pos = pos;
      this.sn = sn;
    }
  }
  
  public static Position getPosition(NodeWindow nw, int x, int y) {
    Vec<Spec> ss = new Vec<>();
    int depth = 0;
    Spec textS = null;
    
    Node c = nw.base;
    while (true) {
      Node n = c.findCh(x, y);
      if (n==null) break;
      x-= n.dx;
      y-= n.dy;
      
      int tyi = c.id("select");
      if (tyi!=-1) {
        String ty = c.vs[tyi].val();
        switch (ty) {
          case "v": ss.add(new Spec(depth, c, y < n.h/2? 0 : 1)); break;
          case "h": ss.add(new Spec(depth, c, x < n.w/2? 0 : 1)); break;
          case "text":
            textS = new Spec(depth, c, -1); // keep the latest one
            break;
          default:
            throw new IllegalStateException("invalid 'select' field value '" + ty + "'");
        }
      }
      
      depth++;
      c = n;
    }
    
    if (textS!=null && c instanceof StringNode) {
      StringNode strNode = (StringNode) c;
      Font f = strNode.f;
      for (StringNode.Word w : strNode.words) {
        if (w.type==0) {
          if (w.split == null) {
            if (x>=w.x && x<w.x+w.w && y>=w.y && y<w.y+f.hi) {
              if (w.overkill==null) w.overkill = w.buildPara(strNode);
              textS.pos = w.overkill.getGlyphPositionAtCoordinate(x-w.x, y-w.y).getPosition();
              textS.word = w;
              break;
            }
          } else {
            // int cy = w.y;
            // text(g, w.split[0], f, w.x, cy, color);
            // for (int i = 1; i < w.split.length-1; i++) {
            //   cy+= fh;
            //   text(g, w.split[i], f, 0, cy, color);
            // }
            // cy+= fh;
            // text(g, w.split[w.split.length-1], f, 0, cy+w.bl-f.ascI, color);
          }
        }
      }
      ss.add(textS);
    }
    
    return new Position(c, ss);
  }
}
