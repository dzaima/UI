package dzaima.utils;

public class Box<T> {
  public T v;
  public Box() { }
  public Box(T n) { this.v = n; }
  
  public void set(T n) { this.v = n; }
  public T get() { return v; }
  public boolean has() { return v!=null; }
}
