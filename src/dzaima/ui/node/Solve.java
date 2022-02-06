package dzaima.ui.node;

import dzaima.utils.Vec;

public class Solve {
  // public static int[] solveN(Vec<Node> ch, int x, int aw, boolean y) { // returns ch.sz+1 items, last being sum of minW
  //   int[] res = new int[ch.sz+1];
  //   int sum = 0;
  //   for (int i = 0; i < ch.sz; i++) {
  //     Node c = ch.get(i);
  //     sum+= res[i] = y? c.minH(aw) : c.minW();
  //   }
  //   res[ch.sz] = sum;
  //   return res;
  // }
  public static int[] solve(Vec<Node> ch, int x, int w, boolean y) { // returns ch.sz+1 items, last being sum of minW 
    int l = ch.sz;
    
    int  [] min = new int  [l];   int minSum = 0;
    int  [] max = new int  [l+1]; long maxSum = 0; // max is reused as result; maxSum is long because addition of Integer.MAX_VALUEs is involved
    float[] wgt = new float[l];   float wgtSum = 0;
    boolean[] done = new boolean[l]; int doneCount = 0;
    int lx = x; // space left for undone cols
    for (int i = 0; i < ch.sz; i++) {
      Node c = ch.get(i); int wid = c.id("weight");
      min[i] = y? c.minH(w) : c.minW(); minSum+= min[i];
      max[i] = y? c.maxH(w) : c.maxW(); maxSum+= max[i];
      wgt[i] = wid==-1?1:c.vs[wid].f(); wgtSum+= wgt[i];
      if (min[i]==max[i]) {
        lx-= max[i];
        done[i] = true; doneCount++; wgtSum-= wgt[i];
      }
    }
    
    max[l] = minSum;
    assert x >= minSum : "Solve - invalid width ("+x+" < "+minSum+")";
    if (maxSum < x) return max;
    loop: while (doneCount!=l) {
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
          if (n>max[i] || min[i]>n) { System.err.println("oh no :'("); continue loop; }
          lx-= min[i] = max[i] = n;
          done[i] = true; doneCount++; wgtSum-= wgt[i];
        }
        break;
      }
    }
    
    return max;
  }
}
