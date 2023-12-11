package dzaima.ui.gui.select;

import dzaima.ui.gui.Font;
import dzaima.ui.node.Node;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.StringNode.Word;
import dzaima.utils.Vec;
import io.github.humbleui.skija.paragraph.Paragraph;

public class Position {
  public final Node n; // deepest matching node
  public final Vec<PosPart> ss;
  protected Position(Node n, Vec<PosPart> ss) {
    this.n = n;
    this.ss = ss;
  }
  
  
  
  public static Position make(Selectable s, Node n, int pos) {
    return new Position(n, Vec.of(new PosPart(0, s, pos, n)));
  }
  
  public static Position getPosition(Node c, int fx, int fy) {
    Vec<PosPart> ss = new Vec<>();
    int depth = 0;
    Vec<PosPart> textTodo = new Vec<>();
    
    while (true) {
      Node n = c.nearestProperCh(fx, fy);
      if (n==null) break;
      fx-= n.dx;
      fy-= n.dy;
      
      if (c instanceof Selectable && ((Selectable) c).selectable()) {
        Selectable s = (Selectable) c;
        switch (s.selType()) { default: throw new IllegalStateException();
          case "v": ss.add(new PosPart(depth, s, fy < n.h/2? 0 : 1, n)); break;
          case "h": ss.add(new PosPart(depth, s, fx < n.w/2? 0 : 1, n)); break;
          case "text": ss.add(textTodo.add(new PosPart(depth, s, -1, null))); break; // keep the latest one
        }
      }
      nextText(n, textTodo, fx, fy);
      
      depth++;
      c = n;
    }
    // assert textTodo.sz == 0;
    for (PosPart p : textTodo) { // TODO proper fallback
      p.ln = c;
      p.pos = 0;
    }
    return new Position(c, ss);
  }
  
  private static void nextText(Node c, Vec<PosPart> textTodo, int fx, int fy) {
    if (textTodo.sz>0) {
      int rPos = -1;
      if (c instanceof StringNode) {
        StringNode str = (StringNode) c;
        Font f = str.f;
        int fh = f.hi;
        int sum = 0;
        for (int wp = 0; wp < str.words.length; wp++) {
          Word w = str.words[wp];
          int n = -1;
          w: {
            int wx = (int) w.x;
            int wy = w.y;
            if (w.split == null) {
              n = wPos(fx, fy, str, w, -1, wx, wy, wy+fh, (int) Math.ceil(w.w));
            } else {
              int i = 0;
              String cs = w.split[i];
              n = wPos(fx, fy, str, w, i, wx, wy, wy+fh, f.width(cs));
              if (n!=-1) break w;
              if (w.f(Word.F_SL)) wy = str.sY2;
              else wy+= fh;
              for (i++; i < w.split.length-1; i++) {
                cs = w.split[i];
                n = wPos(fx, fy, str, w, i, 0, wy, wy+fh, f.width(cs));
                if (n!=-1) break w;
                wy+= fh;
              }
              boolean e = w.f(Word.F_EL);
              cs = w.split[i];
              n = wPos(fx, fy, str, w, i, 0, e? str.eY1 : wy, e? str.eY2 : wy+fh, f.width(cs));
            }
          }
          if (n!=-1) {
            rPos = n+sum;
            break;
          }
          sum+= w.s.length();
        }
        if (rPos==-1) rPos = str.s.length();
      } else if (!(c instanceof InlineNode)) {
        rPos = fy<0? 0 : fy>c.h? 1 : fx>c.w/2? 1 : 0;
      }
      if (rPos!=-1) {
        for (PosPart p : textTodo) {
          p.pos = rPos;
          p.ln = c;
        }
        textTodo.clear();
      }
    }
  }
  
  private static int wPos(int px, int py, StringNode nd, Word c, int spl, int x, int sy, int ey, int w) {
    if (spl<=0 && c.f(Word.F_SL)) { // if these don't hold, assume wPos has been given correct sy/ey
      sy = nd.sY1;
      ey = nd.sY2;
    }
    if (py>=ey) return -1;
    if (py<sy) return 0;
    if (px>=x+w) return -1;
    if (px<x) return 0;
    if (spl<0) {
      if (c.overkill==null) c.overkill = c.buildPara(nd);
      return c.overkill.getGlyphPositionAtCoordinate(px-x, 1).getPosition();
    } else {
      Paragraph p = nd.buildPara(c.split[spl]);
      int r = p.getGlyphPositionAtCoordinate(px-x, 1).getPosition();
      p.close();
      for (int i = 0; i < spl; i++) r+= c.split[i].length();
      return r;
    }
  }
  
  
  
  public static Selection select(Position a, Position b) {
    int bD = -1;
    PosPart aB = null;
    PosPart bB = null;
    int ai = 0;
    int bi = 0;
    while (ai!=a.ss.sz && bi!=b.ss.sz) {
      PosPart ac = a.ss.get(ai);
      PosPart bc = b.ss.get(bi);
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
