import subprocess
import sys

bench = sys.argv[1]
code = sys.argv[2]

settings=['oracle', 'dynamic']

for setting in settings:
    cmd = 'python ./ursaCipaPts.py '+bench + ' ' +setting + ' '+code
    print 'Running '+cmd
    process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE) 
    process.wait()
    print process.returncode
