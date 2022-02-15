package dzaima.ui.gui.select;

import dzaima.ui.gui.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.StringNode;
import dzaima.utils.Vec;
import io.github.humbleui.skija.paragraph.Paragraph;

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
    public int pos;
    public StringNode ln; // leaf node for string
    
    public Spec(int depth, Node sn, int pos) {
      this.depth = depth;
      this.pos = pos; // potentially overwritten later
      this.sn = sn;
    }
  }
  
  public static Position getPosition(NodeWindow nw, int fx, int fy) {
    Vec<Spec> ss = new Vec<>();
    int depth = 0;
    Spec textS = null;
    
    Node c = nw.base;
    while (true) {
      Node n = c.findCh(fx, fy);
      if (n==null) break;
      fx-= n.dx;
      fy-= n.dy;
      
      int tyi = c.id("select");
      if (tyi!=-1) {
        String ty = c.vs[tyi].val();
        switch (ty) {
          case "v": ss.add(new Spec(depth, c, fy < n.h/2? 0 : 1)); break;
          case "h": ss.add(new Spec(depth, c, fx < n.w/2? 0 : 1)); break;
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
      textS.ln = strNode;
      // TODO gracefully handle the case when no word matches 
      Font f = strNode.f;
      int fa = f.ascI;
      int fh = f.hi;
      int sum = 0;
      for (int wp = 0; wp < strNode.words.length; wp++) {
        StringNode.Word w = strNode.words[wp];
        int n = -1;
        w: if (w.type==0) {
          int wx = (int) w.x;
          int wy = w.y;
          if (w.split == null) {
            n = wPos(fx, fy, strNode, w, -1, wx, wy+w.bl-fa, (int) Math.ceil(w.w), fh);
          } else {
            wy-= fa;
            int i = 0;
            String cs = w.split[i];
            n = wPos(fx, fy, strNode, w, i, wx, wy, f.width(cs), fh);
            if (n!=-1) break w;
            for (i++; i < w.split.length-1; i++) {
              wy+= fh;
              cs = w.split[i];
              n = wPos(fx, fy, strNode, w, i, 0, wy, f.width(cs), fh);
              if (n!=-1) break w;
            }
            wy+= fh + w.bl - fa;
            cs = w.split[i];
            n = wPos(fx, fy, strNode, w, i, 0, wy, f.width(cs), fh);
          }
        }
        if (n!=-1) {
          textS.pos = n+sum;
          break;
        }
        sum+= w.s.length();
      }
      ss.add(textS);
    }
    
    return new Position(c, ss);
  }
  private static int wPos(int fx, int fy, StringNode nd, StringNode.Word c, int spl, int x, int y, int w, int h) {
    if (fx>=x && fy>=y && fx<x+w && fy<y+h) {
      if (spl<0) {
        if (c.overkill==null) c.overkill = c.buildPara(nd);
        return c.overkill.getGlyphPositionAtCoordinate(fx-x, 1).getPosition();
      } else {
        Paragraph p = nd.buildPara(c.split[spl]);
        int r = p.getGlyphPositionAtCoordinate(fx-x, 1).getPosition();
        p.close();
        for (int i = 0; i < spl; i++) r+= c.split[i].length();
        return r;
      }
    }
    return -1;
  }
  
  
  
  public static Selection select(Position a, Position b) {
    int bD = -1;
    Spec aB = null;
    Spec bB = null;
    int ai = 0;
    int bi = 0;
    while (ai!=a.ss.sz && bi!=b.ss.sz) {
      Spec ac = a.ss.get(ai);
      Spec bc = b.ss.get(bi);
      if (ac.sn==bc.sn) {
        bD = ac.depth;
        aB = ac;
        bB = bc;
      }
      if (ac.depth>bc.depth) bi++;
      else ai++;
    }
    if (bD==-1) return null;
    return new Selection(bD, a, b, aB, bB);
  }
  
}
