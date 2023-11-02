package dzaima.ui.node;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.utils.*;
import java.util.*;

public class Solve {
  static class Ent implements Comparable<Ent> {
    final double m;
    final int idx;
    Ent(int idx, double m) { this.m=m; this.idx=idx; }
    
    public int compareTo(Ent o) { return Double.compare(m, o.m); }
  }
  
  public static int[] solve(Vec<Node> ch, int x, int widthArg, boolean y) { // returns ch.sz+1 items, last being sum of minW
    int l = ch.sz;
    if (l==0) return new int[]{0};
    
    int[]   min = new int[l];
    int[]   max = new int[l];
    float[] wgt = new float[l];
    long minSum=0, maxSum=0;
    byte[] state = new byte[l];
    int lx = x; // flexible space left
    double lw = 0; // sum of left nodes
    Vec<Ent> vs = new Vec<>();
    
    for (int i = 0; i < l; i++) {
      Node c = ch.get(i); int wid = c.id("weight");
      int minV = min[i] = y? c.minH(widthArg) : c.minW(); minSum+= minV;
      int maxV = max[i] = y? c.maxH(widthArg) : c.maxW(); maxSum+= maxV;
      float wV = wgt[i] = wid==-1? 1 : c.vs[wid].f();
      lx-= minV;
      if (minV == maxV) {
        state[i] = 2;
      } else {
        state[i] = 0;
        assert minV < maxV;
        float wi = 1f/wV;
        vs.add(new Ent(i, (double)minV * wi));
        vs.add(new Ent(i, (double)maxV * wi));
      }
    }
    int[] res = new int[l+1];
    res[l] = (int) minSum;
    if (maxSum <= x) {
      System.arraycopy(max, 0, res, 0, l);
      return res;
    }
    if (x <= minSum) {
      if (x < minSum) Log.warn("solver", "Solver was given impossible request - x="+x+", sum(min)="+minSum);
      System.arraycopy(min, 0, res, 0, l);
      return res;
    }
    
    vs.sort();
    
    int ei = 0;
    while (ei < vs.sz) {
      Ent v = vs.get(ei);
      if (v.m*lw > lx) break;
      
      int i = v.idx;
      int s0 = state[i];
      
      state[i] = (byte) (s0+1);
      if (s0==0) {
        lx+= min[i];
        lw+= wgt[i];
      } else {
        assert s0==1;
        lx-= max[i];
        lw-= wgt[i];
      }
      
      ei++;
    }
    
    double m = lx/lw;
    
    int sum = 0;
    for (int i = 0; i < l; i++) {
      int r;
      switch (state[i]) { default: throw new IllegalStateException();
        case 0:
          r = min[i];
          break;
        case 1:
          int minV = min[i];
          int maxV = max[i];
          int nv = (int) Math.floor(m*wgt[i]);
          if (nv < minV-1 || nv > maxV) Log.warn("solver", "Bad entry: "+nv+" ("+m+"*"+wgt[i]+") not in "+minV+"…"+maxV);
          r = Tools.constrain(nv, minV, maxV);
          break;
        case 2:
          r = max[i];
          break;
      }
      res[i] = r;
      sum+= r;
    }
    
    if (sum < x) {
      int todo = x-sum;
      for (int i = l-1; i >= 0; i--) { // reverse to match old solver; could perhaps order by fractional part of m*wgt[i] but whatever
        if (state[i]==1 && res[i]<max[i]) {
          res[i]++; sum++;
          if (0 == --todo) break;
        }
      }
    }
    
    
    boolean bad = false;
    for (int i = 0; i < l; i++) {
      bad|= res[i]<min[i];
      bad|= res[i]>max[i];
    }
    if (bad || sum > x) {
      Log.warn("solver", "Solver of "+l+" elements failed targeting "+x);
      Log.warn("solver", "min="+Arrays.toString(min)+" max="+Arrays.toString(max)+" wgt="+Arrays.toString(wgt));
      System.arraycopy(min, 0, res, 0, l);
    }
    
    
    return res;
  }
  
  
  
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
  
  
  
  public static int vBinSearch(Vec<Node> nds, int y) {
    int s = 0;
    int e = nds.sz;
    while (s+1<e) {
      int m = (s+e) / 2;
      Node c = nds.get(m);
      if (c.dy<=y) s = m;
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
}
