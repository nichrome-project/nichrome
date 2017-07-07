import subprocess
import sys

bench = sys.argv[1]
code = sys.argv[2]

#settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons', 'uniform']
#uniform can take a lot of iterations, do not include by default
settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons']


for setting in settings:
    cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting +' '+code
    print 'Running '+cmd
    process = subprocess.Popen(cmd, shell=True) 
    process.wait()
    print process.returncode

