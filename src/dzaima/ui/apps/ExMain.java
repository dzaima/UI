package dzaima.ui.apps;

import dzaima.ui.apps.devtools.Devtools;
import dzaima.ui.apps.fmgr.FMgr;
import dzaima.ui.eval.*;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.gui.select.*;
import dzaima.ui.node.*;
import dzaima.ui.node.ctx.*;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.code.CodeAreaNode;
import dzaima.utils.*;

import java.nio.file.*;
import java.util.Arrays;

public class ExMain extends NodeWindow {
  public static int dtc = 0;
  
  public ExMain(GConfig gc, Ctx pctx, PNodeGroup g, String title) {
    super(gc, pctx, g, new WindowInit(title));
  }
  
  public static void run(Windows mgr, String mode) {
    GConfig gc = GConfig.newConfig(gc0 -> {
      gc0.addCfg(() -> Tools.readRes("examples.dzcfg"));
      gc0.addCfg(() -> {
        Path LOCAL_CFG = Paths.get("local.dzcfg");
        if (Files.exists(LOCAL_CFG)) return Tools.readFile(LOCAL_CFG);
        return "";
      });
    });
    BaseCtx ctx = Ctx.newCtx();
    
    
    
    NodeWindow w;
    if (mode.equals("file-manager")) {
      w = FMgr.create(gc, Paths.get("."));
      
    } else if (mode.startsWith("examples/")) {
      gc.addCfg(() -> Tools.readFile(Paths.get(mode))); gc.reloadCfg();
      if (mode.equals("examples/solverTests.dzcfg")) ctx.put("format", FormatNode::new);
      w = new ExMain(gc, ctx, gc.getProp("example.ui").gr(), "example window");
      
    } else {
      w = new ExMain(gc, ctx, Prs.parseNode(Tools.readFile(Paths.get("examples/edit.dzcfg"))), "example window");
      CodeAreaNode ed = (CodeAreaNode) w.base.ctx.id("code");
      ed.setLang(w.gc.langs().fromName("java"));
      int s = ed.um.pushIgnore();
      ed.append(Tools.readFile(Paths.get("src/dzaima/ui/node/types/editable/EditNode.java")));
      ed.um.popIgnore(s);
    }
    
    
    
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
  
  private static class FormatNode extends WrapNode {
    public FormatNode(Ctx ctx, Props props) {
      super(ctx, props);
      String s = getProp("str").str();
      for (Pair<String, Prop> c : props.entries()) {
        if (c.a.charAt(0)=='v') s = s.replace('%'+c.a.substring(1), c.b.toString().replaceAll(".*=", ""));
      }
      add(new StringNode(ctx, s));
    }
  }
  
  static boolean printKeys;
  public static void main(String[] args) {
    if (args.length!=0 && args[0].equals("lwjgl")) {
      Windows.setManager(Windows.Manager.LWJGL);
      args = Arrays.copyOfRange(args, 1, args.length);
    }
    String mode = args.length==0? "" : args[0];
    if (mode.equals("keys")) printKeys = true;
    Windows.start(w -> ExMain.run(w, mode));
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (printKeys) System.out.println(key.repr());
    switch (gc.keymap(key, a, "example.custom")) {
      case "reloadCfg": gc.reloadCfg(); return true;
      case "openDevtools": createTools(); return true;
      case "toggleLegacyStringRendering": StringNode.PARAGRAPH_TEXT^= true; gc.cfgUpdated(); return true;
      case "fontPlus":  gc.setEM(gc.em+1); return true;
      case "fontMinus": gc.setEM(gc.em-1); return true;
    }
    
    return super.key(key, scancode, a);
  }
  
  
  public void tick() {
    super.tick();
    // code for examples/selection.dzcfg
    Node info = base.ctx.idNullable("selectInfo");
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