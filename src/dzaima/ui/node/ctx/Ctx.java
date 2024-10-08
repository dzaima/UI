package dzaima.ui.node.ctx;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.*;
import dzaima.ui.node.types.editable.code.*;
import dzaima.ui.node.types.table.*;
import dzaima.ui.node.types.tabs.*;
import dzaima.ui.node.types.tree.*;
import dzaima.utils.Vec;

import java.util.*;

public abstract class Ctx {
  public abstract NodeGen getGen(String name);
  private final HashMap<String, Node> ids = new HashMap<>();
  public Node idNullable(String id) {
    return ids.get(id);
  }
  public Node id(String id) {
    Node res = idNullable(id);
    assert res!=null : "Expected node with ID "+id+", but it wasn't found";
    return res;
  }
  public GConfig gc;
  public Ctx(GConfig gc) {
    this.gc = gc;
  }
  
  public NodeWindow win() { return null; }
  public NodeVW vw() { return null; }
  
  
  public void focus(Node n) {
    win().focus(n);
  }
  public Node focusedNode() {
    return win()._focusNode;
  }
  
  
  public Ctx shadow() {
    return new ShadowCtx(this);
  }
  public Node make(PNodeGroup pn) {
    return shadow().makeHere(pn);
  }
  public Node make(PNodeGroup pn, HashMap<String, Var> vars) {
    return shadow().makeHere(pn, vars);
  }
  public Node makeKV(PNodeGroup pn, Object... kv) {
    HashMap<String, Var> vars = new HashMap<>();
    for (int i = 0; i < kv.length; i+= 2) {
      Object v = kv[i+1];
      vars.put((String) kv[i], new Var(new ObjProp(v), NO_VARS));
    }
    return make(pn, vars);
  }
  
  public Prop[] finishPropList(PNodeGroup g, HashMap<String, Var> vars) {
    if (g.staticProps!=null) return g.vs;
    Prop[] vs = g.vs;
    for (int i = 0; i < vs.length; i++) {
      if (vs[i]==null) {
        for (vs = vs.clone(); i < vs.length; i++) {
          if (vs[i]==null) {
            PrsField f = g.props.get(i);
            if (f instanceof PrsField.VarFld) {
              vs[i] = getVar(vars, ((PrsField.VarFld) f).s).val;
            } else {
              vs[i] = Prop.makeProp(gc, f);
            }
          }
        }
        return vs;
      }
    }
    return vs;
  }
  
  public Props finishProps(PNodeGroup g, HashMap<String, Var> vars) {
    return g.staticProps!=null? g.staticProps : Props.ofKV(g.ks, finishPropList(g, vars));
  }
  
  
  
  public static class Var {
    boolean used;
    final Prop val;
    final HashMap<String, Var> ctx;
    private Var(Prop val, HashMap<String, Var> ctx) { this.val = val; this.ctx = ctx; }
  }
  public static final HashMap<String, Var> NO_VARS = new HashMap<>();
  private static Var getVar(HashMap<String, Var> vars, String k) {
    if (vars==null) throw new Error("Variable '"+k+"' not found - no variables available");
    Var v = vars.get(k);
    if (v==null) throw new Error("Variable '"+k+"' not found");
    v.used = true;
    return v;
  }
  
  
  public Node makeHere(PNode pn, HashMap<String, Var> vars) {
    Vec<Node> nodes = new Vec<>();
    HashMap<String, Prop> props = new HashMap<>();
    makeHere(pn, vars, nodes, props);
    if (!props.isEmpty() || nodes.sz!=1) throw new Error("Expected to instantiate single node");
    return nodes.get(0);
  }
  public Node makeHere(PNode pn) {
    return makeHere(pn, NO_VARS);
  }
  
  private String rmPrefix(String k) {
    return k.startsWith("$")? k.substring(1) : k;
  }
  private void makeHere(PNode pn, HashMap<String, Var> vars, Vec<Node> resList, HashMap<String, Prop> resProps) {
    if (pn instanceof PNodeGroup) {
      PNodeGroup g = (PNodeGroup) pn;
      
      if (g.defn && g.name.contains(".")) {
        PropI p = gc.getProp(g.name);
        HashMap<String, Var> args = new HashMap<>();
        Prop[] vs = finishPropList(g, vars);
        for (int i = 0; i < g.ks.length; i++) {
          String k = g.ks[i];
          args.put(rmPrefix(k), new Var(vs[i], vars));
        }
        makePropHere(p, args, resList, resProps);
      } else {
        NodeGen gen;
        if (g.defn) {
          Var var = vars.get(rmPrefix(g.name));
          if (var==null) throw new Error("Path '"+g.name+"' not found for generation");
          gen = var.val.obj();
        } else {
          gen = getGen(g.name);
        }
        if (gen==null) throw new Error(g.name==null? "Encountered node with no name" : "no node defined by name '"+g.name+"'");
        
        Vec<Node> chList = new Vec<>();
        HashMap<String, Prop> chProps = new HashMap<>();
        for (PNode c : g.ch) makeHere(c, vars, chList, chProps);
        
        String[] ks = g.ks;
        Props props1;
        if (!chProps.isEmpty()) {
          int i = ks.length;
          HashSet<String> pks = new HashSet<>(Arrays.asList(ks));
          ks = Arrays.copyOf(ks, i+chProps.size());
          Prop[] vs = finishPropList(g, vars);
          vs = Arrays.copyOf(vs, ks.length);
          for (Map.Entry<String, Prop> e : chProps.entrySet()) {
            if (pks.contains(e.getKey())) throw new Error("Multiple definitions of property '"+e.getKey()+"'");
            ks[i] = e.getKey();
            vs[i] = e.getValue();
            i++;
          }
          assert Vec.of(vs).indexOf(null) == -1;
          props1 = Props.ofKV(ks, vs);
        } else {
          props1 = finishProps(g, vars);
        }
        
        Node nd = gen.make(this, props1);
        Prop idProp = props1.getNullable("id");
        if (idProp!=null) {
          String id = idProp.val();
          if (id==null) throw new Error("id property must be an enum");
          if (ids.put(id, nd)!=null) throw new Error("Multiple elements with the id "+id);
        }
        
        for (Node c : chList) nd.add(c);
        resList.add(nd);
      }
    } else if (pn instanceof PNode.PNodeStr) {
      resList.add(new StringNode(this, ((PNode.PNodeStr) pn).s));
    } else if (pn instanceof PNode.PNodeDefn) {
      Var v = getVar(vars, ((PNode.PNodeDefn) pn).s);
      makePropHere(v.val, v.ctx, resList, resProps);
    }
  }
  
  private void makePropHere(Prop val, HashMap<String, Var> args, Vec<Node> resList, HashMap<String, Prop> resProps) { // try to expand a Prop to a node
    if (val instanceof GrProp) {
      PNodeGroup g = val.gr();
      if (g.name!=null) {
        makeHere(g, args, resList, resProps);
      } else {
        for (PNode c : g.ch) makeHere(c, args, resList, resProps);
        Prop[] vs = finishPropList(g, args);
        for (int i = 0; i < vs.length; i++) {
          if (resProps.put(g.ks[i], vs[i])!=null) throw new Error("Multiple definitions of property '"+g.ks[i]+"'");
        }
      }
    } else if (val instanceof StrProp) {
      resList.add(new StringNode(this, ((StrProp) val).s));
    } else if (val instanceof ObjProp && val.obj() instanceof Node) {
      resList.add(val.obj());
    } else throw new Error(val.toString()+" isn't a node type");
  }
  
  
  public static class WindowCtx extends Ctx {
    private final Ctx p;
    public NodeVW vw;
    
    public WindowCtx(GConfig gc, Ctx p, NodeVW vw) {
      super(gc);
      this.p = p;
      this.vw = vw;
    }
    
    public NodeWindow win() { return vw.w; }
    public NodeVW vw() { return vw; }
    
    public NodeGen getGen(String name) {
      return p.getGen(name);
    }
  }
  static class ShadowCtx extends Ctx {
    private final Ctx p;
    
    public ShadowCtx(Ctx p) {
      super(p.gc);
      this.p = p;
    }
    
    public NodeWindow win() { return p.win(); }
    public NodeVW vw() { return p.vw(); }
    
    public NodeGen getGen(String name) {
      return p.getGen(name);
    }
  }
  
  public interface NodeGen {
    Node make(Ctx ctx, Props props);
  }
  
  
  
  
  
  
  public static BaseCtx newCtx() {
    HashMap<String, Ctx.NodeGen> map = new HashMap<>();
    map.put("text", TextNode::new);
    map.put("stext", STextNode::new);
    map.put("ta", InlineNode.TANode::new);
    map.put("h", HNode::new);
    map.put("v", VNode::new);
    map.put("hl", HlNode::new);
    map.put("vl", VlNode::new);
    map.put("btn", BtnNode::new);
    map.put("checkbox", CheckboxNode::new);
    map.put("radio", RadioNode::new);
    map.put("pad", PadNode::new);
    map.put("svgicon", SvgIconNode::new);
    map.put("textarea", TextAreaNode::new);
    map.put("textfield", TextFieldNode::new);
    map.put("codearea", CodeAreaNode::new);
    map.put("codefield", CodeFieldNode::new);
    map.put("menufield", MenuFieldNode::new);
    map.put("scroll", ScrollNode::new);
    map.put("reorderable", ReorderableNode::new);
    map.put("tree", TreeNode::new);
    map.put("tn", TNNode::new);
    map.put("table", TableNode::new);
    map.put("tr", TRNode::new);
    map.put("th", THNode::new);
    map.put("hsep", HSepNode::new);
    map.put("vsep", VSepNode::new);
    map.put("seph", HSepNode::new);
    map.put("sepv", VSepNode::new);
    map.put("img", ImgNode::new);
    map.put("hide", HideNode::new);
    map.put("overlap", OverlapNode::new);
    map.put("menu", MenuNode::new);
    map.put("mi", MenuNode.MINode::new);
    map.put("weighed", WeighedNode::new);
    map.put("tabbed", TabbedNode::new);
    map.put("windowsplit", WindowSplitNode::new);
    map.put("label", LabelNode::new);
    return new BaseCtx(map);
  }
}
