package dzaima.ui.gui;

import dzaima.ui.eval.PNodeGroup;
import dzaima.ui.gui.Popup;
import dzaima.ui.gui.config.*;
import dzaima.ui.node.ctx.Ctx;
import dzaima.utils.*;

import java.util.function.Predicate;

public class PartialMenu {
  public final PNodeGroup gr;
  public final Vec<Predicate<String>> consumers = new Vec<>();
  
  public PartialMenu(GConfig gc) {
    gr = gc.getProp("partialMenu.menu").gr().copy();
  }
  
  public void add(PNodeGroup l, Predicate<String> consume) {
    gr.ch.addAll(l.ch);
    consumers.add(consume);
  }
  public void add(PNodeGroup l, String k, Runnable r) {
    add(l, s -> {
      if (!s.equals(k)) return false;
      r.run();
      return true;
    });
  }
  
  public void open(Ctx ctx) {
    if (gr.ch.sz!=0) Popup.rightClickMenu(ctx.gc, ctx, gr, s -> {
      for (Predicate<String> c : consumers) {
        if (c.test(s)) return;
      }
      if (s.equals("(closed)")) return;
      Log.warn("partialmenu", "Nothing consumed menu item '" + s + "'!");
    });
  }
}
