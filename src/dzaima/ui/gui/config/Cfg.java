package dzaima.ui.gui.config;

import dzaima.ui.eval.*;
import dzaima.ui.gui.io.Key;
import dzaima.ui.node.prop.*;
import dzaima.utils.Tools;

import java.util.*;

public class Cfg {
  public final Cfg p;
  public final String name, path;
  public final HashMap<String, PropI> props = new HashMap<>();
  public final HashMap<String, Cfg> sub = new HashMap<>();
  
  public Cfg(Cfg p, String name) {
    this.p = p;
    this.name = name;
    this.path = p==null? "" : p.subpathName(name);
  }
  
  
  public String subpathName(String v) {
    if (path.length()==0) return v;
    else return path+"."+v;
  }
  
  public PropI get(String path) {
    String[] p = Tools.split(path, '.');
    Cfg c = getSubByPath(p, 1, false);
    if (c!=null) {
      PropI r = c.props.get(p[p.length-1]);
      if (r!=null) return r;
    }
    throw new Error("Config path '"+path+"' found");
  }
  
  private HashMap<String, String> kbActions;
  public String action(String key) {
    if (kbActions==null) {
      kbActions = new HashMap<>();
      Cfg kt = getSubByName("keys", false);
      if (kt==null) throw new RuntimeException("Didn't find 'keys' at '"+path+"' for keymap");
      kt.props.forEach((name, gr) -> {
        for (PNode n : gr.gr().ch) {
          String c = ((PNodeStr) n).s;
          Key k = Key.byName(c);
          if (k==null) throw new RuntimeException("Invalid key description: \""+c+"\"");
          kbActions.put(k.repr(), name);
        }
      });
    }
    return kbActions.get(key);
  }
  
  
  
  
  void update(CfgBuilder b, PNodeGroup g) {
    for (PrsField f : g.props) updateField(this, b, f);
    
    for (PNode c0 : g.ch) {
      if (!(c0 instanceof PNodeGroup)) throw new RuntimeException("Unexpected string in config");
      PNodeGroup c = (PNodeGroup) c0;
      String newPath = c.name;
      Cfg sub = getSubByPath(newPath, true);
      sub.update(b, c);
    }
  }
  
  public Cfg getSubByPath(String s, boolean create) {
    return getSubByPath(Tools.split(s, '.'), 0, create);
  }
  public Cfg getSubByPath(String[] path, int drop, boolean create) {
    Cfg c = this;
    for (int i = 0; i < path.length-drop; i++) {
      c = c.getSubByName(path[i], create);
      if (!create && c == null) return null;
    }
    return c;
  }
  public Cfg getSubByName(String s, boolean create) {
    if (create) return sub.computeIfAbsent(s, k->new Cfg(this, k));
    else return sub.get(s);
  }
  
  public PropI maybeGetProp(String path) {
    String[] p = Tools.split(path, '.');
    Cfg c = getSubByPath(p, 1, false);
    if (c==null) return null;
    return c.props.get(p[p.length-1]);
  }
  
  private static void updateField(Cfg parent, CfgBuilder b, PrsField f) {
    String fullPath = parent.subpathName(f.name);
    String[] p = Tools.split(f.name, '.');
    Cfg c = parent.getSubByPath(p, 1, true);
    String last = p[p.length - 1];
    if (!b.currentlyAdded.add(fullPath)) throw new RuntimeException("Duplicate config key '"+fullPath+"'");
    if (f instanceof PrsField.NameFld && ((PrsField.NameFld) f).cfg) {
      b.toMap.put(fullPath, ((PrsField.NameFld) f).s);
    } else {
      c.props.put(last, (PropI) Prop.makeProp(b.gc, f));
    }
  }
  
  public String toString() {
    StringBuilder r = new StringBuilder("{\n");
    props.forEach((k, v) -> r.append("  ").append(k).append(" = ").append(v.toString().replace("\n", "\n  ")).append('\n'));
    sub  .forEach((k, v) -> r.append("  ").append(k).append(' '  ).append(v.toString().replace("\n", "\n  ")).append('\n'));
    return r.append('}').toString();
  }
}
