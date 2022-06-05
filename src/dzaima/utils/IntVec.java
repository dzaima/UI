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
  public void addAll(int i, int[] t) {
    while (sz+t.length > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+t.length, sz-i);
    System.arraycopy(t, 0, arr, i, t.length);
    sz+= t.length;
  }
  
  
  private void dcap() {
    int[] narr = new int[arr.length<<1 | 7];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = narr;
  }
}
