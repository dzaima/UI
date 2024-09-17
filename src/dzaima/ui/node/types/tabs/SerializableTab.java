package dzaima.ui.node.types.tabs;

import dzaima.ui.eval.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.utils.*;

import java.util.HashMap;
import java.util.function.Function;

public interface SerializableTab {
  default String serialize() { return ""; }
  String serializeName();
  
  static String serializeTree(Node n) {
    return serializeTree(n, v -> { throw new IllegalStateException("Cannot serialize "+(v==null? "null" : v.getClass().getName())); });
  }
  static String serializeTree(Node n, Function<Object, String> more) {
    StringBuilder b = new StringBuilder();
    _serializeAdd_rec(b, n, 0, more);
    return b.toString();
  }
  
  static HashMap<String, Prop> props(Ctx ctx, PNodeGroup g) {
    HashMap<String, Prop> r = new HashMap<>();
    for (Pair<String, Prop> c : ctx.finishProps(g, null).entries()) {
      r.put(c.a, c.b);
    }
    return r;
  }
  static Node deserializeTree(Ctx ctx, String s, HashMap<String, Function<HashMap<String, Prop>, Tab>> ctors) {
    PNodeGroup g = Prs.parseNode(s);
    return _deserialize_rec(ctx, g, ctors);
  }
  
  
  static Node _deserialize_rec(Ctx ctx, PNodeGroup g, HashMap<String, Function<HashMap<String, Prop>, Tab>> ctors) {
    HashMap<String, Prop> p = props(ctx, g);
    switch (g.name) {
      case "split": {
        WindowSplitNode r = new WindowSplitNode(ctx, Props.of("dir", new EnumProp(p.get("d").val())));
        r.setModifiable(p.get("e").b());
        r.setWeight(p.get("w").f());
        for (PNode c : g.ch) {
          if (!(c instanceof PNodeGroup)) throw new IllegalStateException("Expected all children to be groups");
          r.add(_deserialize_rec(ctx, (PNodeGroup) c, ctors));
        }
        return r;
      }
      case "tabbed": {
        TabbedNode tn = new TabbedNode(ctx);
        String m = p.get("m").val();
        tn.setMode(m.equals("a")? TabbedNode.Mode.ALWAYS : m.equals("m")? TabbedNode.Mode.WHEN_MULTIPLE : TabbedNode.Mode.NEVER);
        int sel = p.get("s").i();
        int i = 0;
        for (PNode c : g.ch) {
          if (!(c instanceof PNodeGroup)) throw new IllegalStateException("Expected all children to be groups");
          PNodeGroup g2 = (PNodeGroup) c;
          Function<HashMap<String, Prop>, Tab> ctor = ctors.get(g2.name);
          Tab t;
          if (ctor==null) {
            if ("group".equals(g2.name)) {
              assert g2.ch.sz==1;
              t = new TabbedNode.GroupTab(props(ctx, g2).get("n").str(), _deserialize_rec(ctx, (PNodeGroup) g2.ch.get(0), ctors));
            } else {
              throw new IllegalStateException("Deserialization constructor for '"+g2.name+"' not found");
            }
          } else {
            t = ctor.apply(props(ctx, g2));
          }
          if (t!=null) {
            TabWrapper w = tn.addTab(t);
            if (i==sel) tn.toTab(w);
          } else {
            Log.warn("tab deserialize", "Couldn't deserialize "+g2.name);
          }
          i++;
        }
        return tn;
      }
      default:
        throw new IllegalStateException("bad node name " + g.name);
    }
  }
  
  
  static void _serializeAdd_rec(StringBuilder b, Node nd, int depth, Function<Object, String> more) {
    final int INDENT = 2;
    String is = Tools.repeat(' ', depth);
    
    b.append(is);
    if (nd instanceof WindowSplitNode) {
      WindowSplitNode s = (WindowSplitNode) nd;
      b.append("split { ").append("e=").append(s.isModifiable()).append(" w=").append(s.getWeight()).append(" d=").append(s.isVertical()? "v" : "h").append('\n');
      for (int i = 0; i < 2; i++) _serializeAdd_rec(b, s.ch.get(i), depth+INDENT, more);
      b.append(is).append("}");
    } else if (nd instanceof TabbedNode) {
      TabbedNode tn = (TabbedNode) nd;
      String mode = tn.mode==TabbedNode.Mode.ALWAYS? "a" : tn.mode==TabbedNode.Mode.WHEN_MULTIPLE? "m" : "n";
      Tab[] tabs = tn.getTabs();
      Tab ctab = tn.cTab();
      b.append("tabbed { m=").append(mode).append(" s=").append(ctab==null? -1 : Vec.ofReuse(tabs).indexOf(ctab)).append('\n');
      for (Tab t : tabs) {
        b.append(is).append("  ");
        if (t instanceof SerializableTab) {
          SerializableTab s = (SerializableTab) t;
          b.append(s.serializeName()).append(" {");
          String ct = s.serialize();
          if (ct.indexOf('\n')==-1) {
            b.append(' ').append(ct).append(ct.isEmpty()? "}" : " }");
          } else {
            if (ct.endsWith("\n")) ct = ct.substring(0, ct.length()-1);
            b.append("\n    ").append(is).append(ct.replace("\n", "\n    "+is));
            b.append("\n  ").append(is).append('}');
          }
        } else if (t instanceof TabbedNode.GroupTab) {
          b.append("group { n=").append(JSON.quote(((TabbedNode.GroupTab) t).name)).append("\n");
          _serializeAdd_rec(b, ((TabbedNode.GroupTab) t).getContent(), depth+INDENT+INDENT, more);
          b.append(is).append(Tools.repeat(' ', INDENT)).append("}");
        } else {
          b.append(more.apply(t));
        }
        b.append('\n');
      }
      b.append(is).append("}");
    } else {
      b.append(more.apply(nd));
    }
    b.append('\n');
  }
}
