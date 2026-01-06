package dzaima.ui.node.utils;

import dzaima.ui.gui.Graphics;
import dzaima.ui.node.Node;
import dzaima.utils.*;

public class ListUtils {
  public static int vMinW(Vec<Node> nds) {
    int r = 0;
    for (Node c : nds) r = Math.max(c.minW(), r);
    return r;
  }
  
  public static int vMinH(Vec<Node> nds, int w) {
    int r = 0;
    for (Node c : nds) r+= c.minH(w);
    return r;
  }
  
  public static int hMinW(Vec<Node> nds) {
    int r = 0;
    for (Node c : nds) r+= c.minW();
    return r;
  }
  
  public static int hMinH(Vec<Node> nds, int w) { // TODO this is evil? see adacb997e1b3a90e076a81cd6b7ae803232f95a1
    int r = 0;
    for (Node c : nds) r = Math.max(c.minH(w), r);
    return r;
  }
  
  public static int vMaxW(Vec<Node> nds) {
    int r = 0;
    for (Node c : nds) r = Math.max(c.maxW(), r);
    return r;
  }
  
  public static int vMaxH(Vec<Node> nds, int w) {
    int r = 0;
    for (Node c : nds) {
      r+= c.maxH(w);
      if (r>=Tools.BIG || r<0) return Tools.BIG;
    }
    return r;
  }
  
  public static int hMaxW(Vec<Node> nds) {
    int r = 0;
    for (Node c : nds) {
      r+= c.maxW();
      if (r>=Tools.BIG || r<0) return Tools.BIG;
    }
    return r;
  }
  
  public static int hMaxH(Vec<Node> nds, int w) {
    int r = 0;
    for (Node c : nds) r = Math.max(c.maxH(w), r);
    return r;
  }
  
  public static void vDrawCh(Graphics g, boolean full, Node n) {
    Vec<Node> ch = n.ch;
    if (g.clip==null) {
      for (int i=0; i<ch.sz; i++) ch.get(i).draw(g, full);
    } else {
      for (int i = vBinSearch(ch, g.clip.sy); i<ch.sz; i++) {
        Node c = ch.get(i);
        if (c.dy+c.h < g.clip.sy) continue;
        if (c.dy > g.clip.ey) break;
        c.draw(g, full);
      }
    }
  }
  public static void hDrawCh(Graphics g, boolean full, Node n) {
    Vec<Node> ch = n.ch;
    if (g.clip==null) {
      for (int i=0; i<ch.sz; i++) ch.get(i).draw(g, full);
    } else {
      for (int i = hBinSearch(ch, g.clip.sx); i<ch.sz; i++) {
        Node c = ch.get(i);
        if (c.dx+c.w < g.clip.sx) continue;
        if (c.dx > g.clip.ex) break;
        c.draw(g, full);
      }
    }
  }
  public static void drawCh(Graphics g, boolean full, Node n, boolean v) {
    if (v) vDrawCh(g, full, n);
    else hDrawCh(g, full, n);
  }
  
  public static int vBinSearch(Vec<Node> nds, int y) {
    int s = 0;
    int e = nds.sz;
    while (s+1<e) {
      int m = (s+e) / 2;
      if (nds.get(m).dy<=y) s = m;
      else e = m;
    }
    return s;
  }
  public static int hBinSearch(Vec<Node> nds, int x) {
    int s = 0;
    int e = nds.sz;
    while (s+1<e) {
      int m = (s+e) / 2;
      if (nds.get(m).dx<=x) s = m;
      else e = m;
    }
    return s;
  }
  
  public static Node findNearestLinear(Vec<Node> nds, int x, int y) {
    if (nds.sz<2) return nds.sz==0 || nds.get(0).w==-1? null : nds.get(0);
    int min = Integer.MAX_VALUE, curr;
    Node best = null;
    for (Node c : nds) if (c.w!=-1 && (curr=XY.dist(x, y, c.dx, c.dy, c.w, c.h))<min) { min=curr; best = c; }
    return best;
  }
  
  public static Node vFindNearest(Vec<Node> nds, int x, int y) {
    if (nds.sz<20) return findNearestLinear(nds, x, y);
    return nds.get(vBinSearch(nds, y));
  }
  public static Node hFindNearest(Vec<Node> nds, int x, int y) {
    if (nds.sz<20) return findNearestLinear(nds, x, y);
    return nds.get(hBinSearch(nds, y));
  }
  public static Node findNearest(Vec<Node> nds, int x, int y, boolean v) {
    return v? vFindNearest(nds, x, y) : hFindNearest(nds, x, y);
  }
}
