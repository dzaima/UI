package dzaima.utils;

import java.util.*;
import java.util.function.*;

@SuppressWarnings("unchecked")
public final class Vec<T> implements Iterable<T> {
  T[] arr; // null for alwaysEmpty()
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
  private Vec(Void aVoid) {
    this.arr = null;
    sz = 0;
  }
  
  public T get(int i) {
    assert i < sz;
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
    assert i<=sz;
    if (++sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    arr[i] = t;
    return t;
  }
  public void insert(int i, Vec<T> t) {
    assert i<=sz;
    int osz = sz;
    sz+= t.sz;
    while (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+t.sz, osz-i);
    System.arraycopy(t.arr, 0, arr, i, t.sz);
  }
  public void set(int i, T t) {
    assert i<sz;
    arr[i] = t;
  }
  public int remove(T c) { // returns index of item deleted
    int i = 0;
    while (arr[i]!=c) i++;
    System.arraycopy(arr, i+1, arr, i, sz-i-1);
    arr[--sz] = null;
    return i;
  }
  public T[] get(int s, int e, Class<T[]> c) {
    assert e<=sz;
    return Arrays.copyOfRange(arr, s, e, c);
  }
  public void remove(int s, int e) {
    assert e<=sz;
    System.arraycopy(arr, e, arr, s, sz-e);
    int nsz = sz - (e-s);
    Arrays.fill(arr, nsz, sz, null);
    sz = nsz;
  }
  public void removeAt(int i) {
    assert i < sz;
    System.arraycopy(arr, i+1, arr, i, sz-i-1);
    arr[--sz] = null;
  }
  public <Q extends T> void addAll(int i, Q[] t) {
    addAll(i, t, 0, t.length);
  }
  public <Q extends T> void addAll(Q[] t) {
    addAll(sz, t, 0, t.length);
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
    assert i <= sz;
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
  public boolean isEmpty() {
    return sz==0;
  }
  
  public T[] toArray(Object[] arr) { // guarantees returning a new instance
    return (T[]) Arrays.copyOf(this.arr, sz, arr.getClass());
  }
  public Collection<T> collectionView() {
    return new AbstractCollection<T>() {
      public Iterator<T> iterator() { return Vec.this.iterator(); }
      public int size() { return sz; }
    };
  }
  public ArrayList<T> asArrayList() {
    ArrayList<T> l = new ArrayList<>();
    l.addAll(collectionView());
    return l;
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
  
  public static <T> boolean allEquals(Vec<T> a, Vec<T> b) {
    if (a.sz != b.sz) return false;
    for (int i = 0; i < a.sz; i++) {
      if (!Objects.equals(a.arr[i], b.arr[i])) return false;
    }
    return true;
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
  
  private static final Vec<?> EMPTY_VEC = new Vec<>((Void) null);
  public static <T> Vec<T> frozenEmpty() {
    return (Vec<T>) EMPTY_VEC;
  }
  
  public static <T> Vec<T> ofIterable(Iterable<T> vs) {
    Vec<T> r = new Vec<>();
    for (T v : vs) r.add(v);
    return r;
  }
  public static <T> Vec<T> init(int size, IntFunction<T> nth) {
    T[] elts = (T[]) new Object[size];
    for (int i = 0; i < size; i++) elts[i] = nth.apply(i);
    return Vec.ofReuse(elts);
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
