package dzaima.utils;

import java.util.Arrays;

public class IntVec {
  private static final int[] EMPTY = new int[0];
  public int[] arr = EMPTY;
  public int sz;
  
  public int get(int i) {
    return arr[i];
  }
  
  public void add(int t) {
    if (sz < arr.length) {
      arr[sz] = t;
      sz++;
    } else {
      dcap();
      arr[sz++] = t;
    }
  }
  public int pop() {
    return arr[--sz];
  }
  
  
  
  public void insert(int i, int t) {
    if (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    sz++;
    arr[i] = t;
  }
  public int[] get(int s, int e) {
    return Arrays.copyOfRange(arr, s, e);
  }
  public int[] get() {
    return Arrays.copyOf(arr, sz);
  }
  public void remove(int s, int e) {
    System.arraycopy(arr, e, arr, s, sz-e);
    sz-= e-s;
  }
  public void addAll(int i, int[]  t) { addAll(i, t,     0, t.length); }
  public void addAll(int i, IntVec t) { addAll(i, t.arr, 0, t.sz); }
  public void addAll(int[]  t) { addAll(sz, t,     0, t.length); }
  public void addAll(IntVec t) { addAll(sz, t.arr, 0, arr.length); }
  public void addAll(int i, IntVec t, int s, int e) { addAll(i, t.arr, s, e); }
  public void addAll(int i, int[]  t, int s, int e) {
    int l = e-s;
    while (arr.length < sz+l) dcap();
    System.arraycopy(arr, i, arr, i+l, sz-i);
    System.arraycopy(t, s, arr, i, l);
    sz+= l;
  }
  
  
  
  private void dcap() {
    int[] narr = new int[arr.length<<1 | 7];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = narr;
  }
}
