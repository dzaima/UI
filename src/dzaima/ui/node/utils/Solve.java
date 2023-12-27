package dzaima.ui.node.utils;

import dzaima.ui.node.Node;
import dzaima.ui.node.prop.Prop;
import dzaima.utils.*;

import java.util.Arrays;

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
      Node c = ch.get(i); Prop weight = c.getPropN("weight");
      int minV = min[i] = y? c.minH(widthArg) : c.minW(); minSum+= minV;
      int maxV = max[i] = y? c.maxH(widthArg) : c.maxW(); maxSum+= maxV;
      float wV = wgt[i] = weight==null? 1 : weight.f();
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
      if (x < minSum) throw new RuntimeException("Solver was given impossible request - x="+x+", sum(min)="+minSum);
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
}
