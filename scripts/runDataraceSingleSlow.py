import subprocess
import sys

bench = sys.argv[1]
code = sys.argv[2]

settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons', 'uniform']

for setting in settings:
    cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting +' '+code+' 1'
    print 'Running '+cmd
    process = subprocess.Popen(cmd, shell=True) 
    process.wait()
    print process.returncode

