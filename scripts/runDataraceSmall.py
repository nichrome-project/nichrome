import subprocess
import sys

code = sys.argv[1]

benches=['sor','elevator','raytracer','montecarlo']

settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting + + ' '+code
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True) 
        process.wait()
        print process.returncode

bench = 'raytracer'

setting = 'uniform'

cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting + + ' '+code
print 'Running '+cmd
process = subprocess.Popen(cmd, shell=True) 
process.wait()
print process.returncode
