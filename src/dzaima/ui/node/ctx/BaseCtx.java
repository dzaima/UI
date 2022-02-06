package dzaima.ui.node.ctx;

import java.util.HashMap;

public class BaseCtx extends Ctx {
  private final HashMap<String, NodeGen> map;
  
  public BaseCtx(HashMap<String, NodeGen> map) {
    super(null);
    this.map = map;
  }
  
  public NodeGen getGen(String name) {
    return map.get(name);
  }
  
  public void put(String k, NodeGen v) {
    if (map.get(k) != null) throw new Error("redefining " + k);
    map.put(k, v);
  }
}
