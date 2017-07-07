import subprocess
import sys

code = sys.argv[1]

benches=['elevator','montecarlo']

settings=['oracle', 'dynamic']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaCipaPts.py '+bench + ' ' +setting + ' '+code
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE) 
        process.wait()
        print process.returncode
