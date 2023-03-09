package dzaima.ui.gui.undo;

import dzaima.ui.gui.config.GConfig;
import dzaima.utils.*;

import java.util.Objects;

public class UndoManager {
  public GConfig gc;
  public UndoFrame curr;
  
  public final Vec<UndoFrame> us; // list of actual undos
  public final Vec<UndoFrame> usT; // temporary actions by popU
  public final Vec<UndoFrame> rs; // list of actual redos
  
  public UndoManager(GConfig gc) {
    us = new Vec<>();
    usT = new Vec<>();
    rs = new Vec<>();
    this.gc = gc;
  }
  
  public void undo() {
    while (usT.sz>0) usT.pop().undo();
    while (us.sz>0) {
      UndoFrame u = us.pop();
      u.undo();
      rs.add(u);
      if (u.important) break;
    }
  }
  
  public void redo() {
    while (usT.sz>0) usT.pop().undo();
    while (rs.sz>0) {
      UndoFrame r = rs.pop();
      r.redo();
      us.add(r);
      if (rs.sz>0 && rs.peek().important) break;
    }
  }
  
  public void solidifyTemp() {
    for (UndoFrame c : usT) us.add(c);
    usT.clear();
  }
  
  public static long mergeTimeout = 3000;
  
  public <T> void add(UndoR<T> u) {
    if (groupDepth==0) {
      Log.error("UndoManager", "you forgot to start a group!!");
      Log.stacktraceHere("UndoManager");
      System.out.flush(); System.err.flush();
    }
    curr.is.add(u);
  }
  public <T> T u(UndoR<T> u) {
    add(u);
    return u.redoR();
  }
  
  
  public void pushL(String id) { push(id, 2); } // loud frame - every instance must be separately undo/redo-ed
  public void pushQ(String id) { push(id, 1); } // quiet frame - equal ID frames within some timeframe will be grouped together while undoing
  public void pushU(String id) { push(id, 0); } // unimportant frame - doesn't clear redos
  
  public int groupDepth;
  public void push(String id, int mode) {
    if (mode!=0) solidifyTemp();
    if (groupDepth==0) curr = new UndoFrame(id, mode, gc.lastMs);
    groupDepth++;
  }
  
  private boolean forceImportantNext;
  @SuppressWarnings("StringEquality") // no reason to do full string comparison, jvm guarantees constant interning
  public void pop() {
    assert groupDepth>0;
    groupDepth--;
    if (groupDepth!=0) return;
    Vec<UndoFrame> myList = curr.mode==0? usT : us;
    UndoFrame last = myList.peek();
    
    long ms = curr.time;
    curr.important = forceImportantNext  ||  last==null  ||  curr.mode==2  ||  curr.mode==1 && (last.id!=curr.id || ms > last.time+mergeTimeout);
    
    myList.add(curr);
    if (curr.mode!=0) rs.clear();
    
    curr = null;
    forceImportantNext = false;
  }
  
  public int pushIgnore() { // makes edits done just completely disappear. be very careful about this! meant for initializing things on just created objects
    solidifyTemp();
    pushQ("(ignored frame)");
    return curr.is.sz;
  }
  public void popIgnore(int n) {
    while (curr.is.sz>n) curr.is.pop();
    pop();
  }
  
  public void clear() {
    us.clear();
    usT.clear();
    rs.clear();
  }
  
  public void forceBoundary() { // ensure that it's possible to undo/redo to this point
    solidifyTemp();
    // if (us.sz>0) us.peek().important = true;
    forceImportantNext = true;
  }
  
  public Object currMarker() { // returns the currently incomplete segment if one is active, such that onModified can use it
    return curr!=null? curr : usT.sz>0? usT.peek() : us.sz!=0? us.peek() : (Integer) 0;
  }
  public boolean isAtMarker(Object o) {
    return Objects.equals(currMarker(), o);
  }
}
