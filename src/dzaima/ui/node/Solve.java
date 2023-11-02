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
  public static boolean oldSolver;
  public static int[] solve(Vec<Node> ch, int x, int widthArg, boolean y) { // returns ch.sz+1 items, last being sum of minW
    if (oldSolver) return solveOld(ch, x, widthArg, y);
    if (ch.get(0).ctx.win() instanceof Devtools) return solveOld(ch, x, widthArg, y);
    
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
    
    vs.sort();
    
    boolean print = ch.get(0).id("debugme")!=-1;
    if (print) {
      System.out.println();
      System.out.println("tot="+x+" min="+Arrays.toString(min)+" max="+Arrays.toString(max)+" wgt="+Arrays.toString(wgt));
    }
    
    int ei = 0;
    while (ei < vs.sz) {
      Ent v = vs.get(ei);
      if (v.m*lw > lx) break;
      
      int i = v.idx;
      int s0 = state[i];
      
      if (print) System.out.print("ent "+v.idx+" s="+s0+" at "+v.m+" cut="+(int)(lx/lw)+": lx0="+lx+" lw0="+lw);
      state[i] = (byte) (s0+1);
      if (s0==0) {
        lx+= min[i];
        lw+= wgt[i];
      } else {
        assert s0==1;
        lx-= max[i];
        lw-= wgt[i];
      }
      
      if (print) System.out.println(" lx1="+lx+" lw1="+lw);
      
      ei++;
    }
    
    double m = lx/lw;
    if (print) {
      System.out.println("ended at "+ei+"/"+vs.sz+" lx="+lx+" lw="+lw+" m="+m);
      while (ei < vs.size()) {
        Ent v = vs.get(ei);
        System.out.println("..ent "+v.idx+" at "+v.m);
        ei++;
      }
    }
    
    for (int i = 0; i < l; i++) {
      switch (state[i]) {
        case 0:
          res[i] = min[i];
          break;
        case 1:
          int minV = min[i];
          int maxV = max[i];
          int nv = (int) (m*wgt[i]);
          assert nv >= minV-1 && nv <= maxV+1 : nv+" ("+m+"*"+wgt[i]+") not in "+minV+"…"+maxV; // -1/+1 to allow for float inaccuracy
          res[i] = Tools.constrain(nv, minV, maxV);
          break;
        case 2:
          res[i] = max[i];
          break;
      }
    }
    
    // TODO deal with leftover pixels
    
    
    int rs = 0;
    boolean bad = false;
    for (int i = 0; i < l; i++) {
      bad|= res[i]<min[i];
      bad|= res[i]>max[i];
      rs+= res[i];
    }
    if (bad || rs > x) {
      System.out.println(x+" "+Arrays.toString(min)+" "+Arrays.toString(max)+" "+Arrays.toString(wgt));
      System.out.println("  got "+Arrays.toString(res));
      System.out.println("  exp "+Arrays.toString(solveOld(ch, x, widthArg, y)));
    }
    
    
    return res;
  }
  
  
  
  
  
  
  
  
  
  
  
  public static int[] solveOld(Vec<Node> ch, int x, int widthArg, boolean y) { // returns ch.sz+1 items, last being sum of minW
    int l = ch.sz;
    
    int  [] min = new int  [l];   int minSum = 0;
    int  [] max = new int  [l+1]; long maxSum = 0; // max is reused as result; maxSum is long because addition of Integer.MAX_VALUEs is involved
    float[] wgt = new float[l];   float wgtSum = 0;
    boolean[] done = new boolean[l]; int doneCount = 0;
    int lx = x; // space left for undone cols
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i); int wid = c.id("weight");
      min[i] = y? c.minH(widthArg) : c.minW(); minSum+= min[i];
      max[i] = y? c.maxH(widthArg) : c.maxW(); maxSum+= max[i];
      wgt[i] = wid==-1?1:c.vs[wid].f(); wgtSum+= wgt[i];
      if (min[i]==max[i]) {
        lx-= max[i];
        done[i] = true; doneCount++; wgtSum-= wgt[i];
      }
    }
    
    max[l] = minSum;
    assert x >= minSum : "Solve - invalid width ("+x+" < "+minSum+")";
    if (maxSum < x) return max;
    while (doneCount!=l) {
      int over = -1;
      int pdone = doneCount;
      for (int i = 0; i < l; i++) if (!done[i]) {
        float bsz = wgt[i] * lx / wgtSum; // best size; TODO cache 1/wgtSum
        if (bsz < min[i]) {
          lx-= max[i] = min[i];
          done[i]=true; doneCount++; wgtSum-= wgt[i];
        }
        if (bsz > max[i]) over = i;
      }
      if (pdone!=doneCount) continue;
      if (over>=0) {
        lx-= min[over] = max[over];
        done[over]=true; doneCount++; wgtSum-= wgt[over];
      } else {
        for (int i = 0; i < l; i++) if (!done[i]) {
          int n = (int) (lx/wgtSum*wgt[i]);
          if (n>max[i] || min[i]>n) {
            Log.warn("solver", "Solver failed");
            n = min[i];
          }
          lx-= min[i] = max[i] = n;
          done[i] = true; doneCount++; wgtSum-= wgt[i];
        }
        break;
      }
    }
    
    return max;
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
