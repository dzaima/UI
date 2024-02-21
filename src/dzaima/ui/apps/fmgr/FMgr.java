package dzaima.ui.apps.fmgr;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.editable.EditNode;
import dzaima.utils.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FMgr extends NodeWindow {
  final Node ftb;
  final EditNode pathField;
  Path path;
  final Node info;
  final ScrollNode tableScroll;
  Vec<FRef> refs;
  
  public FMgr(GConfig gc, Ctx pctx, PNodeGroup g, Path p) {
    super(gc, pctx, g, new WindowInit("Files"));
    ftb = base.ctx.id("fileTable");
    info = base.ctx.id("info");
    Path home = Paths.get(System.getProperty("user.home"));
    path = p;
    pathField = (EditNode) base.ctx.id("path");
    pathField.setFn((a, mod) -> {
      if (a!=EditNode.EditAction.ENTER) return false;
      String ns = FOp.decodeC(pathField.getAll());
      try {
        if (ns!=null) {
          Path n = Paths.get(ns);
          if (Files.exists(n)) {
            to(n);
            return true;
          }
        }
      } catch (InvalidPathException ignored) { }
      System.err.println("Bad path: "+ns); // TODO dialog
      return true;
    });
    tableScroll = (ScrollNode) base.ctx.id("tableScroll");
    btn("up", b -> to(path.getParent()));
    btn("refresh", b -> load());
    btn("home", b -> to(home));
    btn("term", b -> gc.openTerminal(path));
    to(path);
    thr = Tools.thread(() -> {
      while (true) {
        while (true) {
          FRef c = reqs.poll(); if(c==null) break;
          try {
            // ↓ ಠ_ಠ
            //noinspection ConstantConditions
            c.dirSize.set(c.path.toFile().list().length);
            dirSizeUpd.set(true);
          } catch (Exception ignored) { }
        }
        try {
          //noinspection BusyWait
          Thread.sleep(100); // ↑ yep you're correct; TODO do something better
        } catch (InterruptedException e) { return; }
      }
    });
  }
  
  AtomicBoolean dirSizeUpd = new AtomicBoolean(false);
  public void tick() { super.tick();
    if (dirSizeUpd.compareAndSet(true, false)) { refresh(); }
  }
  
  Thread thr;
  public void stopped() { super.stopped();
    thr.interrupt();
  }
  
  public void refresh() {
    refs.sort((a, b) -> {
      if (a.dir != b.dir) return a.dir? -1 : 1;
      return a.nameLower.compareTo(b.nameLower);
    });
    ftb.remove(1, ftb.ch.sz);
    for (FRef c : refs) ftb.add(c.updNode());
  }
  
  private void btn(String id, Consumer<BtnNode> r) {
    ((BtnNode) base.ctx.id(id)).setFn(r);
  }
  public void to(Path p) {
    to(p, null);
  }
  public void to(Path p, Path from) {
    try {
      path = p.toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      path = p.toAbsolutePath();
    }
    pathField.removeAll();
    pathField.append(FOp.encodeC(path.toString()));
    pathField.um.clear();
    load();
    if (ftb.ch.sz>1) {
      Node n = null;
      if (from!=null) {
        for (Node c : ftb.ch) {
          if (c instanceof FRef.FileRow && ((FRef.FileRow) c).r.path.equals(from)) {
            n = c;
            break;
          }
        }
      }
      if (n==null) n = ftb.ch.get(1);
      focus(n);
    }
  }
  
  private static final Props ITALICS = Props.of("italics", EnumProp.TRUE);
  public void load() {
    info.clearCh();
    tableScroll.toXY0(true);
    clearDirReqs();
    refs = new Vec<>();
    Vec<Path> ch;
    try {
      ch = FOp.list(path);
    } catch (Exception e) {
      TextNode t = new TextNode(info.ctx, ITALICS);
      t.add(new StringNode(info.ctx, "failed to load"));
      info.add(t);
      Log.warn("fmgr", "Failed to get directory listing:");
      Log.stacktraceHere("fmgr");
      refresh();
      return;
    }
    for (Path c : ch) refs.add(new FRef(this, c));
    refresh();
  }
  
  public static NodeWindow create(GConfig gc, Path path) {
    gc.addCfg(() -> Tools.readRes("fmgr.dzcfg"));
    gc.reloadCfg();
    return new FMgr(gc, Ctx.newCtx(), gc.getProp("fmgr.ui").gr(), path);
  }
  
  public boolean key(Key key, int scancode, KeyAction a) {
    if (super.key(key, scancode, a)) return true;
    if (a.press) {
      if (key.onlyAlt()) {
        if (key.k_up()) {
          to(path.getParent(), path);
          return true;
        }
        if (key.k_down()) {
          if (_focusNode instanceof FRef.FileRow) {
            ((FRef.FileRow) _focusNode).action(0);
          }
          return true;
        }
      } else if (key.onlyCtrl()) {
        if (key.k_l()) {
          focus(pathField);
          pathField.keyF(new Key(KeyVal.end, 0), 0, KeyAction.PRESS);
          return true;
        }
      } else if (key.plain()) {
        if (key.k_f12()) { createTools(); return true; }
        if (key.k_f4()) { gc.openTerminal(path); return true; }
        if (key.k_up() || key.k_down()) {
          if (ftb.ch.sz>1) focus(ftb.ch.get(1));
          return true;
        }
      }
      if (key.k_f5()) {
        gc.reloadCfg();
        return true;
      }
    }
    return false;
  }
  
  
  ConcurrentLinkedQueue<FRef> reqs = new ConcurrentLinkedQueue<>();
  public void clearDirReqs() {
    reqs.clear();
  }
  public void addDirReq(FRef r) {
    reqs.add(r);
  }
}
