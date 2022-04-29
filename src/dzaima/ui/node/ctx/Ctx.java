package dzaima.ui.node.ctx;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.node.Node;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.*;
import dzaima.ui.node.types.editable.code.*;
import dzaima.ui.node.types.table.*;
import dzaima.ui.node.types.tree.*;

import java.util.HashMap;

public abstract class Ctx {
  public abstract NodeGen getGen(String name);
  private final HashMap<String, Node> ids = new HashMap<>();
  public Node id(String id) { return ids.get(id); }
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
    return win().focusNode;
  }
  
  
  public Ctx shadow() {
    return new ShadowCtx(this);
  }
  public Node make(PNodeGroup pn) {
    return shadow().makeHere(pn);
  }
  
  public Prop[] finishProps(PNodeGroup g) {
    Prop[] vs = g.vs;
    for (int i = 0; i < vs.length; i++) {
      if (vs[i]==null) {
        for (vs = vs.clone(); i < vs.length; i++) {
          if (vs[i]==null) vs[i] = Prop.makeProp(gc, g.props.get(i));
        }
        return vs;
      }
    }
    return vs;
  }
  
  public Node makeHere(PNode pn) {
    if (pn instanceof PNodeGroup) {
      PNodeGroup g = (PNodeGroup) pn;
      NodeGen nd = getGen(g.name);
      if (nd==null) throw new Error(g.name==null? "Encountered node with no name" : "no node defined by name '"+g.name+"'");
      Prop[] vs = finishProps(g);
      Node res = nd.make(this, g.ks, vs);
      for (int i = 0; i < g.ks.length; i++) {
        if (g.ks[i].equals("id")) {
          String id = vs[i].val();
          if (id==null) throw new Error("id property must be an enum");
          if (ids.put(id, res)!=null) throw new Error("Multiple elements with the id "+id);
          break;
        }
      }
      for (PNode c : g.ch) res.add(makeHere(c));
      return res;
    }
    if (pn instanceof PNodeStr) {
      return new StringNode(this, ((PNodeStr) pn).s);
    }
    throw new RuntimeException("NYI PNode type "+pn.getClass().getSimpleName());
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
    Node make(Ctx ctx, String[] ks, Prop[] vs);
  }
  
  
  
  
  
  
  public static BaseCtx newCtx() {
    HashMap<String, Ctx.NodeGen> map = new HashMap<>();
    map.put("text", TextNode::new);
    map.put("stext", STextNode::new);
    map.put("h", HNode::new);
    map.put("v", VNode::new);
    map.put("hl", HlNode::new);
    map.put("vl", VlNode::new);
    map.put("btn", BtnNode::new);
    map.put("pad", PadNode::new);
    map.put("svgicon", SvgIconNode::new);
    map.put("textarea", TextAreaNode::new);
    map.put("textfield", TextFieldNode::new);
    map.put("codearea", CodeAreaNode::new);
    map.put("codefield", CodeFieldNode::new);
    map.put("scroll", ScrollNode::new);
    map.put("reorderable", ReorderableNode::new);
    map.put("tree", TreeNode::new);
    map.put("tn", TNNode::new);
    map.put("table", TableNode::new);
    map.put("tr", TRNode::new);
    map.put("th", THNode::new);
    map.put("hsep", HSepNode::new);
    map.put("vsep", VSepNode::new);
    map.put("img", ImgNode::new);
    map.put("hide", HideNode::new);
    map.put("overlap", OverlapNode::new);
    map.put("menu", MenuNode::new);
    map.put("mi", MenuNode.MINode::new);
    return new BaseCtx(map);
  }
}
