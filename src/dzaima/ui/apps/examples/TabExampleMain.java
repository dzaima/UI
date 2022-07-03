package dzaima.ui.apps.examples;

import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.prop.Prop;
import dzaima.ui.node.types.StringNode;
import dzaima.ui.node.types.tabs.*;
import dzaima.utils.Tools;

import java.nio.file.Paths;

public class TabExampleMain extends NodeWindow {
  
  public TabExampleMain(GConfig gc, Ctx pctx, PNodeGroup g, String title) {
    super(gc, pctx, g, new WindowInit(title));
  }
  
  public static void run(Windows mgr) {
    GConfig gc = GConfig.newConfig();
    BaseCtx ctx = Ctx.newCtx();
    ctx.put("tabbed", TestTabbedNode::new);
    
    PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/tabs.dzcfg")));
    TabExampleMain w = new TabExampleMain(gc, ctx, g, "example window");
    mgr.start(w);
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (a.press) {
      if (key.k_f12()) {
        createTools();
        return true;
      }
      if (key.k_f5()) {
        gc.reloadCfg();
        base.mResize();
        return true;
      }
      if (key.onlyCtrl()) {
        if (key.k_add())   { gc.setEM(gc.em+1); return true; }
        if (key.k_minus()) { gc.setEM(gc.em-1); return true; }
      }
    }
    return super.key(key, scancode, a);
  }
  
  
  static class TestTabbedNode extends TabbedNode {
    public TestTabbedNode(Ctx ctx, String[] ks, Prop[] vs) {
      super(ctx, ks, vs);
      String n = vs[0].str();
      addTab(new Tab(ctx) { public Node show() { return new StringNode(ctx, "text in "+name()); } public String name() { return n+" tab 1"; } });
      addTab(new Tab(ctx) { public Node show() { return new StringNode(ctx, "text in "+name()); } public String name() { return n+" tab 2"; } });
    }
  }
}