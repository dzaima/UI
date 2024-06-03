package dzaima.utils;

import java.util.*;
import java.util.function.*;

@SuppressWarnings("unchecked")
public final class Vec<T> implements Iterable<T> {
  T[] arr;
  public int sz;
  static Object[] EMPTY = new Object[0];
  public Vec() {
    arr = (T[]) EMPTY;
  }
  public Vec(int capacity) {
    arr = (T[]) new Object[capacity];
  }
  public Vec(Vec<T> copy) {
    arr = Arrays.copyOf(copy.arr, copy.sz);
    sz = copy.sz;
  }
  private Vec(T[] arr) {
    this.arr = arr;
    sz = arr.length;
  }
  public Vec(List<T> arr) {
    this.arr = (T[]) arr.toArray(new Object[0]);
    sz = arr.size();
  }
  
  public T get(int i) {
    return arr[i];
  }
  
  public T pop() {
    return arr[--sz];
  }
  public T peek() {
    return sz==0? null : arr[sz-1];
  }
  
  
  
  public <Q extends T> Q add(Q t) {
    if (sz < arr.length) {
      arr[sz] = t;
      sz++;
    } else {
      dcap();
      arr[sz++] = t;
    }
    return t;
  }
  public <Q extends T> Q insert(int i, Q t) {
    if (++sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    arr[i] = t;
    return t;
  }
  public void insert(int i, Vec<T> t) {
    int osz = sz;
    sz+= t.sz;
    while (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+t.sz, osz-i);
    System.arraycopy(t.arr, 0, arr, i, t.sz);
  }
  public void set(int i, T t) {
    arr[i] = t;
  }
  public int remove(T c) { // returns index of item deleted
    int i = 0;
    while (arr[i]!=c) i++;
    System.arraycopy(arr, i+1, arr, i, sz-i-1);
    sz--;
    return i;
  }
  public T[] get(int s, int e, Class<T[]> c) {
    return Arrays.copyOfRange(arr, s, e, c);
  }
  public void remove(int s, int e) {
    System.arraycopy(arr, e, arr, s, sz-e);
    sz-= e-s;
  }
  public void removeAt(int i) {
    System.arraycopy(arr, i+1, arr, i, sz-i-1);
    sz--;
  }
  public <Q extends T> void addAll(int i, Q[] t) {
    addAll(i, t, 0, t.length);
  }
  public <Q extends T> void addAll(int i, Vec<Q> arr) {
    addAll(i, arr.arr, 0, arr.sz);
  }
  public <Q extends T> void addAll(Vec<Q> arr) {
    addAll(sz, arr.arr, 0, arr.sz);
  }
  public <Q extends T> void addAll(int i, Vec<Q> arr, int s, int e) {
    addAll(i, arr.arr, s, e);
  }
  public <Q extends T> void addAll(int i, Q[] t, int s, int e) {
    int l = e-s;
    while (arr.length < sz+l) dcap();
    System.arraycopy(arr, i, arr, i+l, sz-i);
    System.arraycopy(t, s, arr, i, l);
    sz+= l;
  }
  
  
  public int indexOf(T t) {
    for (int i = 0; i < sz; i++) if (arr[i] == t) return i;
    return -1;
  }
  public int indexOfEqual(T t) {
    for (int i = 0; i < sz; i++) if (Objects.equals(arr[i], t)) return i;
    return -1;
  }
  public boolean every(Predicate<T> f) {
    for (int i = 0; i < sz; i++) if (!f.test(arr[i])) return false;
    return true;
  }
  public boolean some(Predicate<T> f) {
    for (int i = 0; i < sz; i++) if (f.test(arr[i])) return true;
    return false;
  }
  
  
  public int size() {
    return sz;
  }
  
  public T[] toArray(Object[] arr) { // guarantees returning a new instance
    return (T[]) Arrays.copyOf(this.arr, sz, arr.getClass());
  }
  
  
  public <R> Vec<R> map(Function<T, R> f) {
    Vec<R> res = new Vec<>(sz);
    for (int i = 0; i < sz; i++) res.arr[i] = f.apply(arr[i]);
    res.sz = sz;
    return res;
  }
  public Vec<T> filter(Predicate<T> f) {
    Vec<T> res = new Vec<>();
    for (int i = 0; i < sz; i++) {
      T c = arr[i];
      if (f.test(c)) res.add(c);
    }
    return res;
  }
  public void filterInplace(Predicate<T> f) {
    int oi = 0;
    int osz = sz;
    for (int i = 0; i < osz; i++) {
      T c = arr[i];
      if (f.test(c)) arr[oi++] = c;
      else sz--;
    }
  }
  public T linearFind(Predicate<T> f) {
    for (int i = 0; i < sz; i++) {
      T c = arr[i];
      if (f.test(c)) return c;
    }
    return null;
  }
  
  public int binarySearch(Predicate<T> f) { // index of the first entry matching f, or sz if none do
    int s=-1, e=sz; // e incl
    while (s+1<e) {
      int m = (s+e)/2;
      if (f.test(arr[m])) e = m;
      else s = m;
    }
    return e;
  }
  
  
  private void dcap() {
    Object[] narr = new Object[arr.length<<1 | 3];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = (T[]) narr;
  }
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int i = 0;
      public boolean hasNext() { return i < sz; }
      public T next() { return arr[i++]; }
    };
  }
  
  public String toString() {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < sz; i++) {
      if (i!=0) b.append(", ");
      b.append(arr[i]);
    }
    b.append(']');
    return b.toString();
  }
  
  @SafeVarargs
  public static <T> Vec<T> of(T... is) {
    return new Vec<>(is);
  }
  public static <T> Vec<T> ofReuse(T[] vs) {
    return new Vec<>(vs);
  }
  public static <T> Vec<T> ofNew(T[] vs) {
    return new Vec<>(Arrays.copyOf(vs, vs.length));
  }
  public static <T> Vec<T> ofCollection(Collection<T> vs) {
    return ofReuse((T[]) vs.toArray(new Object[0]));
  }
  public static <R, T extends R> Vec<R> ofExCollection(Collection<T> vs) {
    return ofReuse((R[]) vs.toArray(new Object[0]));
  }
  
  public void clear() {
    arr = (T[]) EMPTY;
    sz = 0;
  }
  public void sz0() { // like clear, but keeps the array
    sz = 0;
  }
  
  public void sort() {
    Arrays.sort(arr, 0, sz);
  }
  public void sort(Comparator<T> c) {
    Arrays.sort(arr, 0, sz, c);
  }
  
  public void swap(Vec<T> that) {
    T[] arrm = arr; int szm = sz;
    arr = that.arr; sz = that.sz;
    that.arr = arrm; that.sz = szm;
  }
}
