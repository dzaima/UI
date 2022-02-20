package dzaima.ui.apps.fmgr;

import dzaima.ui.eval.Prs;
import dzaima.ui.gui.Menu;
import dzaima.ui.gui.io.Click;
import dzaima.ui.node.Node;
import dzaima.ui.node.ctx.Ctx;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.*;
import dzaima.ui.node.types.table.TRNode;
import dzaima.utils.Time;

import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class FRef {
  public final FMgr m;
  public final Path path;
  public final TRNode node;
  
  public final String name, nameLower, nameEnc;
  public String size;
  public String type;
  public String time;
  public Instant t;
  public boolean dir;
  public AtomicInteger dirSize;
  
  public FRef(FMgr m, Path path) {
    this.m = m;
    this.path = path;
    this.name = path.getFileName().toString();
    this.nameEnc = FOp.encodeC(name);
    this.nameLower = name.toLowerCase();
    refresh();
    if (dir) {
      dirSize = new AtomicInteger(-1);
      m.addDirReq(this);
    }
    node = new FileRow(this);
  }
  
  public void refresh() {
    type = "";
    time = "";
    size = "";
    t = Instant.EPOCH;
    dir = false;
    try {
      dir = Files.isDirectory(path);
      type = dir? "Folder" : name.indexOf('.')==-1? "File" : name.substring(name.lastIndexOf('.')+1);
      if (Files.isSymbolicLink(path)) type = "Link to "+type;
      t = Files.getLastModifiedTime(path).toInstant();
      time = Time.localTimeStr(t);
      size = dir? "" : String.valueOf(Files.size(path));
    } catch (Exception ignored) { }
  }
  
  public Node updNode() {
    Ctx ctx = m.ftb.ctx;
    node.clearCh();
    node.add(new StringNode(ctx, nameEnc));
    int d = dir? dirSize.get() : -1;
    node.add(new StringNode(ctx, d==-1? size : d+" items"));
    node.add(new StringNode(ctx, type));
    Node tn = new HNode(ctx, new String[]{"alX"}, new Prop[]{new EnumProp("right")});
    tn.add(new StringNode(ctx, time));
    node.add(tn);
    return node;
  }
  
  public static class FileRow extends TRNode {
    public final FRef r;
  
    public FileRow(FRef r) {
      super(r.m.ftb.ctx, Node.KS_NONE, Node.VS_NONE);
      this.r = r;
    }
  
    public void action(int mode) {
      if (mode!=1) {
        if (r.dir) r.m.to(r.path);
        else r.m.gc.openFile(r.path);
      }
    }
    public boolean mouseDown(int x, int y, Click c) {
      if (c.btn==Click.RIGHT) {
        Menu.auto(ctx.win(), this, Prs.parseNode("v { w=10em bg=#222 h{\"New folder\"} h{\"New file\"} }"), false, () -> { });
        return true;
      } else return super.mouseDown(x, y, c);
    }
  }
}
