package dzaima.utils;

import java.util.function.*;

@SuppressWarnings("unchecked")
public class Promise<T> {
  private Object object = new UnresolvedPromise<T>();
  
  private static final class UnresolvedPromise<T> {
    private final Vec<Consumer<T>> consumers = new Vec<>();
  }
  
  public boolean isResolved() {
    return !(object instanceof UnresolvedPromise);
  }
  
  public Promise<T> then(Consumer<T> f) {
    if (isResolved()) f.accept(get());
    else consumers().add(f);
    return this;
  }
  public void set(T val) {
    assert !isResolved();
    Vec<Consumer<T>> tmp = consumers();
    object = val;
    for (Consumer<T> c : tmp) c.accept(val);
  }
  public T get() { // assumes is already resolved
    assert isResolved();
    return (T) object;
  }
  private Vec<Consumer<T>> consumers() {
    return ((UnresolvedPromise<T>) object).consumers;
  }
  
  public static <T> Promise<T> create(Consumer<Promise<T>> r) { // create a new promise `a`, immediately invoke `r` with it, and return `a`
    Promise<T> a = new Promise<>();
    r.accept(a);
    return a;
  }
  
  public static <T> Promise<T> resolved(T value) {
    Promise<T> a = new Promise<>();
    a.set(value);
    return a;
  }
  
  public static <T> Promise<T> all(Supplier<T> f, Promise<?>... ps) {
    Promise<T> res = new Promise<>();
    int[] box = new int[]{ps.length};
    for (Promise<?> c : ps) {
      c.then(x -> {
        if (0 == --box[0]) res.set(f.get());
      });
    }
    return res;
  }
  public static <T> void all(Consumer<Vec<T>> then, Vec<Promise<T>> ps) {
    int[] box = new int[]{ps.sz};
    for (Promise<?> c : ps) {
      c.then(x -> {
        if (0 == --box[0]) then.accept(ps.map(Promise::get));
      });
    }
  }
  
  public static <A,B> void run2(Promise<A> a, Promise<B> b, BiConsumer<A,B> f) {
    a.then(ar -> b.then(br -> f.accept(ar, br)));
  }
  public static <A,B,R> Promise<R> merge2(Promise<A> a, Promise<B> b, BiFunction<A,B,R> f) {
    return Promise.all(() -> f.apply(a.get(), b.get()), a, b);
  }
}
