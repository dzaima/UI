package dzaima.ui.gui.config;

import dzaima.ui.eval.*;
import dzaima.ui.eval.PrsField.NameFld;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.HashSet;

public class Cfg {
  public final Cfg prev;
  public final CfgTree propTree = new CfgTree(this, "");
  
  private final GConfig gc;
  
  private Cfg(GConfig gc, Cfg prev, PNodeGroup g) {
    this.gc = gc;
    this.prev = prev;
    Vec<String> toMapK = new Vec<>();
    Vec<String> toMapV = new Vec<>();
    HashSet<String> dedup = new HashSet<>();
    recInit(toMapK, toMapV, dedup, propTree, "", g);
    for (int i = 0; i < toMapK.sz; i++) { // TODO some proper dependency resolving idk
      String k = toMapK.get(i);
      if (!dedup.add(k)) throw new RuntimeException("Duplicate theme key "+k);
      propTree.add(k, get(toMapV.get(i)));
    }
  }
  
  static String joinPath(String base, String x) {
    return base.isEmpty()? x : base+"."+x;
  }
  private void recInit(Vec<String> toMapK, Vec<String> toMapV, HashSet<String> dedup, CfgTree t, String r, PNodeGroup g) {
    for (PrsField c : g.props) {
      String k = joinPath(r, c.name);
      if (c instanceof NameFld && ((NameFld) c).cfg) {
        toMapK.add(k);
        toMapV.add(((NameFld) c).s);
      } else {
        Prop vR = Prop.makeProp(gc, c);
        PropI v = (PropI)vR;
        t.add(c.name, v);
        if (!dedup.add(k)) throw new RuntimeException("Duplicate theme key "+k);
      }
    }
    for (PNode o : g.ch) {
      if (o instanceof PNodeGroup) {
        PNodeGroup c = (PNodeGroup) o;
        String path = joinPath(r, c.name);
        String[] ps = Tools.split(c.name, '.');
        CfgTree t2 = t.prepTree(ps, ps.length);
        recInit(toMapK, toMapV, dedup, t2, path, c);
      } else if (o instanceof PNodeStr) {
        throw new RuntimeException("Plain string in config");
      }
    }
  }
  
  public PropI get(String path) {
    here: {
      String[] ps = Tools.split(path, '.');
      CfgTree t = propTree;
      for (int i = 0; i < ps.length-1; i++) {
        t = t.sub.get(ps[i]);
        if (t==null) break here;
      }
      PropI v = t.prop(ps[ps.length-1]);
      if (v!=null) return v;
    }
    if (prev!=null) return prev.get(path);
    throw new Error("No config property "+path+" is defined");
  }
  
  public static Cfg add(GConfig gc, Cfg prev, String src) {
    return new Cfg(gc, prev, Prs.parseList(src));
  }
}
