package dzaima.ui.apps;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.apps.fmgr.FMgr;
import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.*;

import java.nio.file.Paths;

public class ExMain extends NodeWindow {
  public static int dtc = 0;
  
  public ExMain(GConfig gc, Ctx pctx, PNodeGroup g, String title) {
    super(gc, pctx, g, new WindowInit(title));
  }
  
  public static void run(Windows mgr) {
    GConfig gc = GConfig.newConfig();
    BaseCtx ctx = Ctx.newCtx();
    
    // NodeWindow w = FMgr.create(gc, Paths.get("."));
    
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/layouts.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/tree.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/text.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/unicode.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/selection.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/chat.dzcfg")));
    // PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/scrollTest.dzcfg")));
    // ExMain w = new ExMain(gc, ctx, g, "example window");
    
    ExMain w = new ExMain(gc, ctx, Prs.parseNode(Tools.readFile(Paths.get("examples/edit.dzcfg"))), "example window");
    CodeAreaNode ed = (CodeAreaNode) w.base.ctx.id("code");
    ed.setLang(w.gc.langs().fromName("java"));
    int s = ed.um.pushIgnore();
    ed.append(Tools.readFile(Paths.get("src/dzaima/ui/node/types/editable/EditNode.java")));
    ed.um.popIgnore(s);
    
    
    if (dtc == 0) {
      mgr.start(w);
    } else {
      w.impl.init.setWindowed(Rect.xywh(0, 0, 800, 852));
      mgr.start(w);
      Devtools d1 = Devtools.create(w);
      d1.impl.init.setWindowed(Rect.xywh(800, 0, 800, 852));
      mgr.start(d1);
      if (dtc == 2) {
        Devtools d2 = Devtools.create(d1);
        d2.impl.init.setWindowed(Rect.xywh(0, 0, 800, 852));
        mgr.start(d2);
      }
    }
  }
  
  public static void main(String[] args) {
    Windows.setManager(Windows.Manager.JWM);
    Windows.start(ExMain::run);
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (a.press) {
      if (key.k_f12()) {
        createTools();
        return true;
      }
      if (key.k_f5()) {
        gc.reloadCfg();
        base.mRedraw();
        base.mResize();
        return true;
      }
      if (key.k_f2()) {
        StringNode.PARAGRAPH_TEXT^= true;
        base.mRedraw();
        return true;
      }
      if (key.onlyCtrl()) {
        if (key.k_add())   { gc.setEM(gc.em+1); return true; }
        if (key.k_minus()) { gc.setEM(gc.em-1); return true; }
      }
    }
    return super.key(key, scancode, a);
  }
  
  
  // code for selection example
  public void tick() {
    super.tick();
    Node info = base.ctx.id("selectInfo");
    if (info!=null) {
      Node t = base.ctx.id("insertText");
      if (t.ch.sz==0) {
        PNodeGroup g = Prs.parseNode(Tools.readFile(Paths.get("examples/text.dzcfg")));
        t.add(ctx.makeHere(((PNodeGroup) ((PNodeGroup) ((PNodeGroup) g.ch.get(0)).ch.get(1)).ch.get(0)).ch.get(1)));
      }
      
      StringBuilder s = new StringBuilder();
      
      if (selection!=null) {
        if (selection.c instanceof InlineNode) s.append("selection: ").append(InlineNode.getSelection(selection)).append('\n');
        s.append("common depth: ").append(selection.depth).append('\n');
      }
      
      Position p = Position.getPosition(base, mx, my);
      s.append("n = ").append(Devtools.name(p.n, false)).append('\n');
      for (PosPart sp : p.ss) {
        s.append("  spec ").append(sp.depth);
        s.append(":");
        s.append(" pos ").append(sp.pos);
        s.append('\n');
      }
      info.replace(0, new StringNode(base.ctx, s.toString()));
    }
  }
}