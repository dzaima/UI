package dzaima.ui.gui;

import dzaima.ui.eval.*;
import dzaima.ui.gui.config.GConfig;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.types.StringNode;
import dzaima.ui.node.types.editable.MenuFieldNode;
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
    if (g.name==null) {
      gr.ch.addAll(g.ch);
    } else {
      gr.ch.add(g);
    }
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
  public String add(String name, Runnable r) {
    String id = "base_gen_"+(counter++);
    PNodeGroup g0 = new PNodeGroup("mi", false, Vec.of(new PrsField.NameFld("id", id)), Vec.of(new PNode.PNodeStr(name)));
    add(new PNodeGroup(null, false, new Vec<>(), Vec.of(g0)), id, r);
    return id;
  }
  
  public void addSep() {
    if (gr.ch.sz!=0) gr.ch.add(gc.getProp("partialMenu.sep").gr());
  }
  
  public void addField(String init, Consumer<String> onModified) {
    add(gc.getProp("menu.menuField").gr(), s -> false);
    onBuild(fm -> {
      MenuFieldNode f = (MenuFieldNode) fm.node.ctx.id("name");
      f.append(init);
      f.onModified = () -> onModified.accept(f.getAll());
    });
  }
  
  private Vec<Consumer<Popup.RightClickMenu>> onBuild = new Vec<>();
  public void onBuild(Consumer<Popup.RightClickMenu> f) { onBuild.add(f); }
  
  public Popup.RightClickMenu open(Ctx ctx, Click cl, Runnable onClose) {
    if (gr.ch.sz!=0) return openCustom(cl, onClose, (gr, onClose2) -> Popup.rightClickMenu(ctx.gc, ctx, gr, onClose2));
    return null;
  }
  public void open(Ctx ctx, Click cl) {
    open(ctx, cl, null);
  }
  
  public static void updateText(Popup.RightClickMenu menu, String id, String text) {
    Ctx ctx = menu.node.ctx;
    ctx.id(id).replace(0, new StringNode(ctx, text));
  }
  
  public <T extends Popup.RightClickMenu> T openCustom(Click cl, Runnable onClose, BiFunction<PNodeGroup, Consumer<String>, T> make) {
    T m = make.apply(gr, s -> {
      if (s.equals("(closed)") && onClose!=null) onClose.run();
      for (Predicate<String> c1 : consumers) {
        if (c1.test(s)) return;
      }
      if (!s.equals("(closed)")) Log.warn("partialmenu", "Nothing consumed menu item '"+s+"'!");
    });
    if (cl != null) m.takeClick(cl);
    for (Consumer<Popup.RightClickMenu> c : onBuild) c.accept(m);
    return m;
  }
}
