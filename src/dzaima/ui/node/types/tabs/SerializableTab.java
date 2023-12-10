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
    serializeAdd_rec(b, n, 0, more);
    return b.toString();
  }
  
  static HashMap<String, Prop> props(PNodeGroup g) {
    HashMap<String, Prop> r = new HashMap<>();
    for (int j = 0; j < g.ks.length; j++) r.put(g.ks[j], g.vs[j]);
    return r;
  }
  static Node deserializeTree(Ctx ctx, String s, HashMap<String, Function<HashMap<String, Prop>, Tab>> ctors) {
    PNodeGroup g = Prs.parseNode(s);
    return deserialize_rec(ctx, g, ctors);
  }
  
  
  static Node deserialize_rec(Ctx ctx, PNodeGroup g, HashMap<String, Function<HashMap<String, Prop>, Tab>> ctors) {
    final String[] WK = new String[]{"dir"};
    HashMap<String, Prop> p = props(g);
    switch (g.name) {
      case "split": {
        WindowSplitNode r = new WindowSplitNode(ctx, WK, new Prop[]{new EnumProp(p.get("d").val())});
        r.setModifiable(p.get("e").b());
        r.setWeight(p.get("w").f());
        for (PNode c : g.ch) {
          if (!(c instanceof PNodeGroup)) throw new IllegalStateException("Expected all children to be groups");
          r.add(deserialize_rec(ctx, (PNodeGroup) c, ctors));
        }
        return r;
      }
      case "tabbed": {
        TabbedNode tn = new TabbedNode(ctx, Node.KS_NONE, Node.VS_NONE);
        String m = p.get("m").val();
        tn.setMode(m.equals("a")? TabbedNode.Mode.ALWAYS : m.equals("m")? TabbedNode.Mode.WHEN_MULTIPLE : TabbedNode.Mode.NEVER);
        int sel = p.get("s").i();
        int i = 0;
        for (PNode c : g.ch) {
          if (!(c instanceof PNodeGroup)) throw new IllegalStateException("Expected all children to be groups");
          PNodeGroup g2 = (PNodeGroup) c;
          Function<HashMap<String, Prop>, Tab> ctor = ctors.get(g2.name);
          if (ctor==null) throw new IllegalStateException("No constructor for name "+g2.name+" found");
          TabWrapper w = tn.addTab(ctor.apply(props(g2)));
          if (i==sel) tn.toTab(w);
          i++;
        }
        return tn;
      }
      default:
        throw new IllegalStateException("bad node name " + g.name);
    }
  }
  
  
  static void serializeAdd_rec(StringBuilder b, Node nd, int depth, Function<Object, String> more) {
    final int INDENT = 2;
    String is = Tools.repeat(' ', depth);
    
    b.append(is);
    if (nd instanceof WindowSplitNode) {
      WindowSplitNode s = (WindowSplitNode) nd;
      b.append("split { ").append("e=").append(s.isModifiable()).append(" w=").append(s.getWeight()).append(" d=").append(s.isVertical()? "v" : "h").append('\n');
      for (int i = 0; i < 2; i++) serializeAdd_rec(b, s.ch.get(i), depth+INDENT, more);
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
