package dzaima.ui.gui.config;

import dzaima.ui.eval.*;
import dzaima.ui.gui.io.Key;
import dzaima.ui.node.prop.PropI;
import dzaima.utils.Tools;

import java.util.HashMap;

public class CfgTree {
  public final Cfg cfg;
  public final String path;
  public final HashMap<String, PropI> props = new HashMap<>();
  public final HashMap<String, CfgTree> sub = new HashMap<>();
  
  CfgTree(Cfg cfg, String path) {
    this.cfg = cfg;
    this.path = path;
  }
  
  public CfgTree getTreePath(String path) {
    CfgTree c = this;
    for (String s : Tools.split(path, '.')) c = c.getTree(s);
    return c;
  }
  public CfgTree getTree(String name) {
    CfgTree r = sub.get(name);
    if (r==null) {
      if (cfg.prev!=null) return cfg.prev.propTree.getTreePath(Cfg.joinPath(path, name));
      throw new RuntimeException("No tree named \""+name+"\" found in \""+path+"\"");
    }
    return r;
  }
  
  public PropI prop(String name) {
    return props.get(name);
  }
  
  CfgTree prepTree(String[] path, int len) {
    CfgTree c = this;
    for (int i = 0; i < len; i++) {
      String s = path[i];
      CfgTree n = c.sub.get(s);
      if (n==null) c.sub.put(s, n = new CfgTree(cfg, Cfg.joinPath(c.path, s)));
      c = n;
    }
    return c;
  }
  void add(String k, PropI v) {
    String[] path = Tools.split(k, '.');
    prepTree(path, path.length-1).props.put(path[path.length-1], v);
  }
  
  private HashMap<String, String> kbActions;
  public String action(String key) {
    if (kbActions==null) {
      kbActions = new HashMap<>();
      CfgTree kt = getTree("keys");
      kt.props.forEach((name, gr) -> {
        for (PNode n : gr.gr().ch) {
          String c = ((PNodeStr) n).s;
          Key k = Key.byName(c);
          if (k==null) throw new RuntimeException("Invalid key: \""+c+"\"");
          kbActions.put(k.repr(), name);
        }
      });
    }
    return kbActions.get(key);
  }
}
