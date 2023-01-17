#!/usr/bin/env python3
import sys, os, urllib.request, subprocess, shutil, hashlib

skija_os = "linux-x64" # or "windows-x64", or "macos-arm64", or "macos-x64"
lwjgl_os = "linux"     # or "windows",     or "macos-arm64", or "macos"
                       # (also hashes below will need to be updated; and there will probably need to be more changes for non-linux OSes)

lwjgl_version = "3.3.0"

def mkdirs(path):
  os.makedirs(path, exist_ok=True)

def call(cmd):
  exit_code = subprocess.call(cmd)
  if exit_code:
    sys.exit(1)
  

def maven_lib(base, name, version, dir, sha256, post = ""):
  jarname = name+"-"+version+post+".jar"
  fname = dir+"/"+jarname
  fname_tmp = fname+".download"
  if not os.path.exists(fname):
    print("downloading "+jarname)
    if os.path.dirname(fname):
      mkdirs(os.path.dirname(fname))
    
    url = "https://repo1.maven.org/maven2/"+base+"/"+name+"/"+version+"/"+jarname
    with open(fname_tmp, 'wb') as f:
      f.write(urllib.request.urlopen(url).read())
    f = open(fname_tmp, "rb")
    sha256got = hashlib.sha256(f.read()).hexdigest()
    if (sha256got != sha256):
      print('unexpected sha256: expected '+sha256+', got '+sha256got)
      sys.exit(1)
    shutil.move(fname_tmp, fname)
  return fname

# note that this function is copied in many places
def git_lib(path, git):
  if os.path.exists(path): return path
  path2 = path+"Clone"
  print("using "+path2+" submodule; link custom path to "+path+" to override")
  subprocess.check_call(["git","submodule","update","--init",path2])
  return path2

def shstr(s):
  return "'"+s.replace("'", "'\\''")+"'"

def jar(res, classpath, release = ""):
  if not 'i' in sys.argv and os.path.exists("classes/"):
    shutil.rmtree("classes/")
  mkdirs("classes/")
  res = os.path.abspath(res)
  srcs = []
  prev_classes = []

  def rec_file(parent, me, left):
    path = parent+me
    src_path = "src"+path
    if os.path.isdir(src_path):
      cls_path = "classes"+path
      mkdirs(cls_path)
      prev_classes.append(cls_path)
      
      files = os.listdir(src_path)
      new_left = [x for x in os.listdir(cls_path) if x.endswith(".class")]
      for f in files:
        new_left = rec_file(path+"/", f, new_left)
      
      for c in new_left:
        # print("removing", "classes"+path+"/"+c)
        os.remove("classes"+path+"/"+c) # remove files that aren't intended to be preserved
      
      return left
    
    if not me.endswith(".java"): raise Exception("unexpected extension for src"+path)
    cls = me[0:-5]
    
    def mine(s):
      return s.startswith(cls+"$") or s==cls+".class"
    my_fs = [x for x in left if mine(x)]
    ot_fs = [x for x in left if not mine(x)]
    
    src_mod = os.path.getmtime(src_path)
    if len(my_fs)==0 or any([os.path.getmtime("classes"+parent+x)<src_mod for x in my_fs]):
      srcs.append(src_path)
      return left
    else:
      return ot_fs
  
  rec_file("", "", [])
  
  if len(srcs)>0:
    # print(srcs)
    call([
      "javac",
      *(["--release",release] if release else []),
      "-classpath", ':'.join(classpath+prev_classes),
      "-Xmaxerrs", "1000",
      "-d", "classes",
      * srcs
    ])
  call(["jar", "cf", res, "-C", "classes", "."])

def build_ui_lib(uiloc):
  mkdirs("lib/")
  uilib = "lib/uilib"
  prev = os.getcwd()
  os.chdir(uiloc)
  cp = build_ui(prev+"/lib/UI.jar")
  os.chdir(prev)
  res = "res/base"
  if not 'keeplib' in sys.argv:
    if os.path.exists(uilib): shutil.rmtree(uilib)
    shutil.copytree(uiloc+"/lib/", uilib)
  if os.path.exists(res): shutil.rmtree(res)
  shutil.copytree(uiloc+"/"+res, res)
  return ["lib/ui"+x for x in cp]+["lib/UI.jar"]

def make_run(path, classpath, main, flags = ""):
  run = f"""#!/usr/bin/env bash
APPDIR=`readlink -f "$0"`
APPDIR=`dirname "$APPDIR"`
java -DRES_DIR="$APPDIR/res/" {flags} -cp {':'.join(['"$APPDIR/"'+shstr(x) for x in classpath])} {main} "$@"
"""

  with open(path, 'w') as f:
    f.write(run)
  os.chmod("run", 0o777)


def build_ui(res = "UI.jar"):
  
  lwjgl_native = "-natives-"+lwjgl_os
  def lwjgl_lib(name, sha256, post = ""):
    return maven_lib("org/lwjgl", name, lwjgl_version, "lib/lwjgl-"+lwjgl_version, sha256, post)
  
  classpath = [
    maven_lib("io/github/humbleui", "types", "0.2.0", "lib", "38d94d00770c4f261ffb50ee68d5da853c416c8fe7c57842f0e28049fc26cca8"),
    maven_lib("io/github/humbleui", "jwm", "0.4.9", "lib", "ee2388a1faa5108ebd0b164df8a533307e2ac8ed5022d4e7078863621635b43f"),
    maven_lib("io/github/humbleui", "skija-shared", "0.109.1", "lib", "c89f87cc208617380c5a5c47b7dfd2a5b1340c1897d135da763703aafa853161"),
    maven_lib("io/github/humbleui", "skija-"+skija_os, "0.109.1", "lib", "d5d658f2c768780adb3cba59c1bcc63a1ac3d13730a5f67fdae00bbf122a5d05"),
    
    lwjgl_lib("lwjgl", "d04bb83798305ffb8322a60ae99c9f93493c7476abf780a1fde61c27e951dd07"),
    lwjgl_lib("lwjgl-glfw", "a4a464130eb8943b41036d9c18f3d94da7aafedec7f407848bbc3c674c93e648"),
    lwjgl_lib("lwjgl-nfd", "64b66ab4e63ca40612c23cab4b4c73be8676396ab1bc7617b364f93703ba3f61"),
    lwjgl_lib("lwjgl-opengl", "0d2b245a1ee269d41a8fb1a194cb848495252ce0cc8222b398e4a9950fbd116c"),
    # lwjgl_lib("lwjgl", "-sources"),
    # lwjgl_lib("lwjgl-glfw", "-sources"),
    # lwjgl_lib("lwjgl-nfd", "-sources"),
    # lwjgl_lib("lwjgl-opengl", "-sources"),
    lwjgl_lib("lwjgl", "ddab8a8ad1e982ef061fe49845bc9010a5b0af3cd563819b8698927e08405f91", lwjgl_native),
    lwjgl_lib("lwjgl-glfw", "9448bcc88acb164183c7b64b2dcb745e38f6cc79a8334c35eb69b245e65869e7", lwjgl_native),
    lwjgl_lib("lwjgl-nfd", "c40cb912c805f35c8a61170d49d22d255b986689f256a8e1e0757b5c484ec8a0", lwjgl_native),
    lwjgl_lib("lwjgl-opengl", "5972d4be0b1b68d86bc979a18e458e5e1e95a63c18fc9efe9c7cec794d5070df", lwjgl_native),
  ]
  
  if not 'skipui' in sys.argv:
    jar(res, classpath, "8")
  return classpath


if __name__ == "__main__":
  cp = build_ui()
  make_run("run", cp+["UI.jar"], "dzaima.ui.apps.ExMain", "-ea")