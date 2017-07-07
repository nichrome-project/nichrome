import subprocess
import sys

code = sys.argv[1]

benches=['sor','elevator','raytracer','montecarlo']

settings=['bagged']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting + ' '+code+' 1'
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True) 
        process.wait()
        print process.returncode
