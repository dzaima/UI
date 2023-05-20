package dzaima.utils;

import java.util.Arrays;

public class ByteVec {
  private static final byte[] EMPTY = new byte[0];
  public byte[] arr;
  public int sz;
  public ByteVec() {
    arr = EMPTY;
  }
  public ByteVec(int prealloc) {
    arr = new byte[prealloc];
  }
  
  public byte get(int i) {
    return arr[i];
  }
  public byte[] get() {
    return Arrays.copyOf(arr, sz);
  }
  
  public void add(byte t) {
    if (sz < arr.length) {
      arr[sz] = t;
      sz++;
    } else {
      dcap();
      arr[sz++] = t;
    }
  }
  public byte pop() {
    return arr[--sz];
  }
  
  
  
  
  
  public byte[] get(int s, int e) {
    return Arrays.copyOfRange(arr, s, e);
  }
  public void insert(int i, byte t) {
    if (sz >= arr.length) dcap();
    System.arraycopy(arr, i, arr, i+1, sz-i);
    sz++;
    arr[i] = t;
  }
  public void remove(int s, int e) {
    System.arraycopy(arr, e, arr, s, sz-e);
    sz-= e-s;
  }
  
  public void insertFill(int i, int am, byte fill) {
    while (sz+ am > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+am, sz-i);
    Arrays.fill(arr, i, i+am, fill);
    sz+= am;
  }
  
  public void addAll(int i, byte[] t, int s, int e) {
    int l = e-s;
    while (sz+l > arr.length) dcap();
    System.arraycopy(arr, i, arr, i+l, sz-i);
    System.arraycopy(t, s, arr, i, l);
    sz+= l;
  }
  public void addAll(int i, byte[] t) { addAll(i, t, 0, t.length); }
  public void addAll(byte[] t, int s, int e) { addAll(sz, t, s, e); }
  public void addAll(byte[] t) { addAll(sz, t); }
  
  private void dcap() {
    byte[] narr = new byte[arr.length<<1 | 7];
    System.arraycopy(arr, 0, narr, 0, arr.length);
    arr = narr;
  }
}
