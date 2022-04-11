package dzaima.ui.gui.config;

import dzaima.ui.gui.Font;
import dzaima.ui.gui.*;
import dzaima.ui.gui.io.*;
import dzaima.ui.node.Node;
import dzaima.ui.node.prop.*;
import dzaima.ui.node.types.editable.code.Langs;
import dzaima.utils.*;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;

public class GConfig {
  public int em = 15;
  public float imgScale = 1;
  public final int framerate = 60; // frames per second; currently unchangeable
  
  public Cfg cfg = null;
  public boolean dragScroll = false; // enable scrolling by dragging (for mobile); may interfere with things expecting draggability
  public long lastNs = System.nanoTime();
  public long lastMs = lastNs/1000000;
  public long deltaNs; // only accurate when last tick did drawing, otherwise it's capped to 1/framerate
  public long cursorOnMs = 500; // TODO theme
  public Font defFont;
  public Vec<NodeWindow> ws = new Vec<>();
  
  private final HashMap<String, CfgProp> cfgMap = new HashMap<>();
  
  public GConfig() {
    addCfg(() -> Tools.readRes("base/default.dzcfg"));
  }
  
  public void setEM(int em) {
    this.em = em;
    imgScale = em>10 && em<16? 1 : 13f/em; // don't scale images if they'd be scaled by too little (to keep pixels real pixels)
    cfgUpdated();
  }
  public void cfgUpdated() {
    defFont = Typeface.of(getProp("str.defaultFont").str()).sizeMode(em, 0);
    for (NodeWindow w : ws) w.cfgUpdated();
  }
  public void initialLoaded() {
    setEM(getProp("str.defaultEM").i());
  }
  public Vec<Supplier<String>> configs = new Vec<>();
  public void addCfg(Supplier<String> src) {
    configs.add(src);
    nextCfg(src.get());
  }
  private void nextCfg(String src) {
    cfg = Cfg.add(this, cfg, src);
  }
  public void reloadCfg() {
    cfg = null;
    for (Supplier<String> c : configs) nextCfg(c.get());
    if (postReload!=null) for (Runnable c : postReload) c.run();
    for (Map.Entry<String, CfgProp> c : cfgMap.entrySet()) {
      c.getValue().init(getProp(c.getKey()));
    }
    cfgUpdated();
  }
  public Vec<Runnable> postReload;
  public void postCfgReload(Runnable r) {
    if (postReload==null) postReload = new Vec<>();
    postReload.add(r);
  }
  
  public void tick(boolean intentionallyLong) {
    long prevNs = lastNs;
    lastNs = System.nanoTime();
    deltaNs = lastNs-prevNs;
    if (intentionallyLong) deltaNs = Math.min(deltaNs, 1000000000/framerate);
    lastMs = lastNs/1000000;
  }
  
  public boolean isClick(Click cl) { // TODO theme?
    return cl.len()<em && (System.currentTimeMillis()-cl.startMs)<200;
  }
  public boolean isDoubleclick(Click cl) { // TODO theme; or move into users, or something that can track that the thing double-clicked is the same thing
    return isClick(cl) && System.currentTimeMillis()-cl.prevMs<400;
  }
  
  
  ///////// themes \\\\\\\\\
  public static GConfig newConfig() {
    GConfig r = new GConfig();
    r.initialLoaded();
    return r;
  }
  public static GConfig newConfig(Consumer<GConfig> init) { // put initial addCfg-s here so they may contribute to one-time initialized things
    GConfig r = new GConfig();
    init.accept(r);
    r.initialLoaded();
    return r;
  }
  public PropI getProp(String path) {
    return cfg.get(path);
  }
  
  public CfgProp getCfgProp(String path) {
    CfgProp p = cfgMap.get(path);
    if (p==null) {
      p = new CfgProp(getProp(path));
      cfgMap.put(path, p);
    }
    return p;
  }
  
  // TODO make these more consistent
  public int col(Node n, String k, String thKey) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].col();
    return getCfgProp(thKey).col();
  }
  public String val(Node n, String k, String thKey) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].val();
    return getCfgProp(thKey).val();
  }
  public String str(Node n, String k, String thKey) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].str();
    return getCfgProp(thKey).str();
  }
  public int len(Node n, String k, String thKey) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    return getCfgProp(thKey).len();
  }
  public float lenF(Node n, String k, String thKey) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    return getCfgProp(thKey).len();
  }
  
  public String valD(Node n, String k, String def) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].val();
    return def;
  }
  public int lenD(Node n, String k, int defPx) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    return defPx;
  }
  public String strD(Node n, String k, String def) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].str();
    return def;
  }
  public int emD(Node n, String k, double d) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    return Tools.ceil(d*em);
  }
  public int pxD(Node n, String k, int i) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    return i;
  }
  public int colD(Node n, String k, int c) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].col();
    return c;
  }
  public int intD(Node n, String k, int i) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].i();
    return i;
  }
  public boolean boolD(Node n, String k, boolean b) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].b();
    return b;
  }
  
  
  public Integer col(Node n, String k) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].col();
    return null;
  }
  public int lenReq(Node n, String k) {
    int id = n.id(k);
    if (id>=0) return n.vs[id].len();
    throw new Error(n.getClass().getSimpleName()+": Property "+k+" required");
  }
  
  public /*open*/ void openFile(Path p) {
    try {
      new ProcessBuilder("xdg-open", p.toAbsolutePath().toString()).start();
    } catch (IOException e) {
      System.err.println("Failed to open file:");
      e.printStackTrace();
    }
  }
  public /*open*/ void openTerminal(Path p) {
    try {
      new ProcessBuilder("/usr/bin/env", "x-terminal-emulator").directory(p.toFile()).start();
    } catch (IOException e) {
      System.err.println("Failed to launch terminal:");
      e.printStackTrace();
    }
  }
  public /*open*/ void openLink(String s) {
    if (!s.startsWith("https://") && !s.startsWith("http://")) return; // don't try to open as file
    Tools.thread(() -> { // start on a new thread because Desktop::browse likes to hang sometimes; TODO detect hanging & fall back
      if (Desktop.isDesktopSupported()) {
        Desktop d = Desktop.getDesktop();
        if (d.isSupported(Desktop.Action.BROWSE)) {
          try {
            d.browse(new URI(s));
            return;
          } catch (IOException | URISyntaxException e) {
            System.err.println("Error on using Desktop::browse or URI::new");
            e.printStackTrace(); // TODO
          };
        }
      }
      System.err.println("open link: fallback to xdg-open");
      try {
        new ProcessBuilder("xdg-open", s).start();
      } catch (IOException e) {
        System.err.println("Failed to open link:");
        e.printStackTrace();
      }
    }, true);
  }
  
  
  public String keymap(Key key, KeyAction a, String root) { // returns empty string if none
    if (!a.typed) return "";
    CfgTree tree = cfg.propTree.getTreePath(root);
    String r = tree.action(key.repr());
    if (r==null) return "";
    return r;
  }
  
  
  private Langs langs;
  public Langs langs() {
    if (langs==null) langs = new Langs();
    return langs;
  }
}
