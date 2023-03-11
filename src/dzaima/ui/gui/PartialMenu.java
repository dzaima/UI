package dzaima.ui.gui;

import dzaima.ui.eval.*;
import dzaima.ui.gui.Popup;
import dzaima.ui.gui.config.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.utils.*;

import java.util.function.*;

public class PartialMenu {
  public final GConfig gc;
  public final PNodeGroup gr;
  public final Vec<Predicate<String>> consumers = new Vec<>();
  
  public PartialMenu(GConfig gc) {
    this.gc = gc;
    gr = gc.getProp("partialMenu.menu").gr().copy();
  }
  
  public void add(PNodeGroup g, Predicate<String> consume) {
    gr.ch.addAll(g.ch);
    consumers.add(consume);
  }
  public void add(PNodeGroup g, String k, Runnable r) {
    add(g, s -> {
      if (!s.equals(k)) return false;
      r.run();
      return true;
    });
  }
  
  private int counter;
  public void add(String name, Runnable r) {
    String id = "base_gen_"+(counter++);
    PNodeGroup g0 = new PNodeGroup("mi", false, Vec.of(new PrsField.NameFld("id", id)), Vec.of(new PNode.PNodeStr(name)));
    add(new PNodeGroup(null, false, new Vec<>(), Vec.of(g0)), id, r);
  }
  
  public void addSep() {
    if (gr.ch.sz!=0) gr.ch.add(gc.getProp("partialMenu.sep").gr());
  }
  
  private Vec<Consumer<Popup.RightClickMenu>> onBuild = new Vec<>();
  public void onBuild(Consumer<Popup.RightClickMenu> f) { onBuild.add(f); }
  public void open(Ctx ctx) {
    if (gr.ch.sz!=0) {
      Popup.RightClickMenu m = Popup.rightClickMenu(ctx.gc, ctx, gr, s -> {
        for (Predicate<String> c : consumers) {
          if (c.test(s)) return;
        }
        if (s.equals("(closed)")) return;
        Log.warn("partialmenu", "Nothing consumed menu item '" + s + "'!");
      });
      for (Consumer<Popup.RightClickMenu> c : onBuild) c.accept(m);
    }
  }
}
