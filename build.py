#!/usr/bin/env python3
import sys, os, platform, urllib.request, subprocess, shutil, hashlib

def fail(msg):
  print(msg)
  sys.exit(1)

def shstr(s):
  return "'"+s.replace("'", "'\\''")+"'"

skip_ui = False
incremental = False
extra_jvm_flags = ''
cmd_prefix = ''
keep_lib = False
lib_os = None
lib_arch = None
components = {
  'jwm': True, # disabling JWM doesn't work, whatever
  'lwjgl': False,
}
for arg in sys.argv[1:]:
  if arg == 'skipui':
    skip_ui = True
  elif arg == 'i':
    incremental = True
  elif arg == 'keeplib':
    keep_lib = True
  elif arg.startswith('jvm-args='):
    extra_jvm_flags += ' '+arg[9:]
  elif arg.startswith('jvm-arg='):
    extra_jvm_flags += ' '+shstr(arg[8:])
  elif arg.startswith('cmd-prefix='):
    cmd_prefix += arg[11:]+' '
  elif arg.startswith('os='):
    lib_os = arg[3:]
    if not lib_os in ['linux', 'macos', 'windows']:
      fail(f'Unknown OS: {lib_os}')
      sys.exit(1)
  elif arg.startswith('arch='):
    lib_arch = arg[5:]
    if not lib_arch in ['x64', 'arm64']:
      fail(f'Unknown architecture: {lib_arch}')
  elif arg.startswith('incl='):
    components[arg[5:]] = True
  elif arg.startswith('excl='):
    components[arg[5:]] = False
  else:
    fail(f'Bad argument: "{arg}"')

if lib_os is None:
  if platform.system() == 'Linux':
    lib_os = 'linux'
  elif platform.system() == 'Darwin':
    lib_os = 'macos'
  elif 'Win' in platform.system():
    lib_os = 'windows'
  else:
    print(f'Unsupported OS "{platform.system()}"; assuming Linux')
    lib_os = 'linux'

if lib_arch is None:
  if platform.machine().lower() in ['x86_64', 'amd64']:
    lib_arch = 'x64'
  elif platform.machine().lower() in ['aarch64', 'arm64']:
    lib_arch = 'arm64'
  else:
    fail(f'Unsupported architecture "{platform.machine()}"')

# lib_os='linux';lib_arch='x86_64'
# lib_os='macos';lib_arch='x86_64'
# lib_os='macos';lib_arch='arm64'
# lib_os='windows';lib_arch='x86_64'

skija_os = lib_os + '-' + lib_arch
lwjgl_os = lib_os + ('' if lib_arch=='x64' else '-'+lib_arch)

lwjgl_version = '3.3.0'

def mkdirs(path):
  os.makedirs(path, exist_ok=True)

def call(cmd):
  exit_code = subprocess.call(cmd)
  if exit_code:
    sys.exit(1)
  

def maven_lib(base, name, version, dir, expected, post = '', repo = 'https://repo1.maven.org/maven2'):
  jar_name = name+'-'+version+post+'.jar'
  fname = dir+'/'+jar_name
  fname_tmp = fname+'.download'
  if not os.path.exists(fname):
    print('downloading '+jar_name)
    if os.path.dirname(fname):
      mkdirs(os.path.dirname(fname))
    
    url = repo+'/'+base+'/'+name+'/'+version+'/'+jar_name
    with open(fname_tmp, 'wb') as f:
      f.write(urllib.request.urlopen(url).read())
    f = open(fname_tmp, 'rb')
    sha256got = hashlib.sha256(f.read()).hexdigest()
    
    if isinstance(expected, str):
      sha256exp = expected
    else:
      sha256exp = [x[2] for x in [x.split('-') for x in expected] if x[0]==lib_os and x[1]==lib_arch][0]
    
    if sha256got != sha256exp:
      print(f'unexpected sha256: got "{sha256got}", expected "{sha256exp}"')
      sys.exit(1)
    shutil.move(fname_tmp, fname)
  return fname

# note that this function is copied in many places
def git_lib(path):
  if os.path.exists(path): return path
  path2 = path+'Clone'
  print(f'using {path2} submodule; link custom path to {path} to override')
  subprocess.check_call(['git', 'submodule', 'update', '--init', path2])
  return path2

def jar(res, classpath, release=''):
  if not incremental and os.path.exists('classes/'):
    shutil.rmtree('classes/')
  mkdirs('classes/')
  res = os.path.abspath(res)
  srcs = []
  prev_classes = []

  def rec_file(parent, me, left):
    path = parent+me
    if not components['lwjgl'] and path in ['/dzaima/ui/gui/lwjgl/LwjglWindow.java', '/dzaima/ui/gui/lwjgl/LwjglManager.java']:
      return left
    
    src_path = 'src'+path
    if os.path.isdir(src_path):
      cls_path = 'classes'+path
      mkdirs(cls_path)
      prev_classes.append(cls_path)

      files = os.listdir(src_path)

      new_left = [x for x in os.listdir(cls_path) if x.endswith('.class')]
      for f in files:
        new_left = rec_file(path+'/', f, new_left)

      for c in new_left:
        # print('removing', f'classes{path}/{c}')
        os.remove(f'classes{path}/{c}') # remove files that aren't intended to be preserved

      return left
    else:
      if not me.endswith('.java'): fail(f'Unexpected extension for src{path}')
      cls = me[0:-5]
  
      def mine(s):
        return s.startswith(cls+'$') or s==cls+'.class'
      my_fs = [x for x in left if mine(x)]
      ot_fs = [x for x in left if not mine(x)]
  
      src_mod = os.path.getmtime(src_path)
      if len(my_fs)==0 or any([os.path.getmtime('classes'+parent+x)<src_mod for x in my_fs]):
        srcs.append(src_path)
        return left
      else:
        return ot_fs
  
  rec_file('', '', [])
  
  if len(srcs)>0:
    # print(srcs)
    call([
      'javac',
      *(['--release', release] if release else []),
      '-classpath', ':'.join(classpath + prev_classes),
      '-Xmaxerrs', '1000',
      '-d', 'classes',
      *srcs
    ])
  call(['jar', 'cf', res, '-C', 'classes', '.'])

def build_ui_lib(uiloc):
  mkdirs('lib/')
  uilib = 'lib/uilib'
  prev = os.getcwd()
  os.chdir(uiloc)
  cp = build_ui(prev + '/lib/UI.jar')
  os.chdir(prev)
  res = 'res/base'
  if not keep_lib:
    if os.path.exists(uilib): shutil.rmtree(uilib)
    shutil.copytree(uiloc+'/lib/', uilib)
  if os.path.exists(res): shutil.rmtree(res)
  shutil.copytree(f'{uiloc}/{res}', res)
  return ['lib/ui'+x for x in cp]+['lib/UI.jar']

def make_run(path, classpath, main, flags = ''):
  flags+= extra_jvm_flags
  run = f"""#!/usr/bin/env bash
APPDIR=`readlink -f "$0"`
APPDIR=`dirname "$APPDIR"`
{cmd_prefix}java -DRES_DIR="$APPDIR/res/" {flags} -cp {':'.join(['"$APPDIR/"'+shstr(x) for x in classpath])} {main} "$@"
"""

  with open(path, 'w') as f:
    f.write(run)
  os.chmod('run', 0o777)


def build_ui(res = 'UI.jar'):
  classpath = [
    maven_lib('io/github/humbleui', 'types', '0.2.0', 'lib', '38d94d00770c4f261ffb50ee68d5da853c416c8fe7c57842f0e28049fc26cca8'),
    maven_lib('io/github/humbleui', 'skija-shared', '0.116.1', 'lib', '27d1575798ab1c8c27f9e9ea8f2b179c2b606dae0ebf136c83b0fbb584ab6da0'),
    maven_lib('io/github/humbleui', 'skija-'+skija_os, '0.116.1', 'lib', ['linux-x64-7c3ab50102ca2b4816954eaeb148fe62458646b2ac6c6611150658f6f8ff5f4b','macos-arm64-307f15824638f5a0d40e0271a7ca5f84d2f155de8caf57d136382b5983ad583e','macos-x64-f675cb22f949ababa2fa4b1999245ff2cba1b6d1b2268a453f1602aeb81716d4','windows-x64-ae333594d148571494aeec9e29c0a9138d9b184120f6932363e2d52730ee17a9']),
  ]
  
  if components['jwm']: classpath+= [
    maven_lib('io/github/humbleui', 'jwm', '0.4.13', 'lib', 'acc22fbb6b2259f26f74a94e5fff17196348a893187d3a4bea9a425f58690596'),
  ]
  
  lwjgl_native = '-natives-'+lwjgl_os
  def lwjgl_lib(name, post, sha256):
    return maven_lib('org/lwjgl', name, lwjgl_version, 'lib/lwjgl-'+lwjgl_version, sha256, post)
  
  # nfd needed by both JWM and LWJGL
  classpath+= [
    lwjgl_lib('lwjgl', '', 'd04bb83798305ffb8322a60ae99c9f93493c7476abf780a1fde61c27e951dd07'),
    lwjgl_lib('lwjgl', lwjgl_native, ['linux-x64-ddab8a8ad1e982ef061fe49845bc9010a5b0af3cd563819b8698927e08405f91','macos-arm64-f42c1a1ab2bbc3e6429817d48990c5f6cd04b284de6a3fe201db0da9901446b0','macos-x64-b2b829074883c1a008b99300092a9b0fb7023c88fe4d041fb32ed7c54ba525f7','windows-x64-cfb0a089cecce866b1c21d5ffb708711d82f059095d81bef842b2c0bd597eb9a']),
    lwjgl_lib('lwjgl-nfd', '', '64b66ab4e63ca40612c23cab4b4c73be8676396ab1bc7617b364f93703ba3f61'),
    lwjgl_lib('lwjgl-nfd', lwjgl_native, ['linux-x64-c40cb912c805f35c8a61170d49d22d255b986689f256a8e1e0757b5c484ec8a0','macos-arm64-ecbab3e2e815a0fdd53a216022abad1f826b92e69e6caeec89cb8cfc1e6c09c1','macos-x64-831ac60d853a6cfbf2932f7462fc21be75b2f086df1eb4c3922f155f1968d77d','windows-x64-88cace1d9baa162fe84f03609ce9a5e065d5834bab944e777462461a5b7b07ad']),
  ]
  
  if components['lwjgl']: classpath+= [
    lwjgl_lib('lwjgl-glfw', '', 'a4a464130eb8943b41036d9c18f3d94da7aafedec7f407848bbc3c674c93e648'),
    lwjgl_lib('lwjgl-opengl', '', '0d2b245a1ee269d41a8fb1a194cb848495252ce0cc8222b398e4a9950fbd116c'),
    lwjgl_lib('lwjgl-glfw',   lwjgl_native, ['linux-x64-9448bcc88acb164183c7b64b2dcb745e38f6cc79a8334c35eb69b245e65869e7','macos-arm64-037fb26882b61749cfa54d1e608d9768a5ec616230911d4d3e02560d2033fca5','macos-x64-928101bde61d2d745b664e3b9e8e2ab9e682553bc8a0be1a42c8874c8c007e61','windows-x64-23954dfa3333a91657cedfca251e147500aa24d14613101d64a326fb0a1fb0f6']),
    lwjgl_lib('lwjgl-opengl', lwjgl_native, ['linux-x64-5972d4be0b1b68d86bc979a18e458e5e1e95a63c18fc9efe9c7cec794d5070df','macos-arm64-83e536559ff292da63381829c9fbf5c64199ac84e55122ed1fa61ec239bd8d6c','macos-x64-a021a0a472bb8a710db4793d674e5f163d2115d00de5002289a206a89796eba8','windows-x64-a364cc3322c0f1a1358988da8f160ad4efec5f6f22bbac9064e8f5836a16d2fe']),
  ]
  # lwjgl_lib('lwjgl', '-sources'),
  # lwjgl_lib('lwjgl-glfw', '-sources'),
  # lwjgl_lib('lwjgl-nfd', '-sources'),
  # lwjgl_lib('lwjgl-opengl', '-sources'),
  
  
  if not skip_ui:
    jar(res, classpath, '8')
  return classpath


if __name__ == '__main__':
  cp = build_ui()
  make_run('run', cp+['UI.jar'], 'dzaima.ui.apps.ExMain', '-ea')