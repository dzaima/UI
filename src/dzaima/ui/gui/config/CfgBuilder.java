package dzaima.ui.gui.config;

import dzaima.ui.eval.Prs;
import dzaima.ui.node.prop.PropI;
import dzaima.utils.Tools;

import java.util.*;

class CfgBuilder {
  public final GConfig gc;
  public final Cfg root = new Cfg(null, "");
  public HashSet<String> currentlyAdded = new HashSet<>();
  public HashMap<String, String> toMap = new HashMap<>();
  
  CfgBuilder(GConfig gc) { this.gc = gc; }
  
  public void addSrc(String src) {
    currentlyAdded.clear();
    root.update(this, Prs.parseList(src));
  }
  
  private PropI complete(HashSet<String> done, String which) {
    boolean shouldExist = !done.add(which);
    PropI res = root.maybeGetProp(which);
    if (res==null && shouldExist) throw new RuntimeException("Dependency cycle involving path '" + which + "'");
    if (res==null) res = complete(done, toMap.get(which));
    
    String[] p = Tools.split(which, '.');
    Cfg c = root.getSubByPath(p, 1, false);
    if (c == null) throw new RuntimeException("Encountered unknown path '" + which + "' while resolving dependencies");
    c.props.put(p[p.length - 1], res);
    return res;
  }
  
  public Cfg complete() {
    HashSet<String> done = new HashSet<>();
    for (String k : toMap.keySet()) {
      if (!done.contains(k)) complete(done, k);
    }
    return root;
  }
}
