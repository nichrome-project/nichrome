import subprocess
import sys

code = sys.argv[1]

benches=['sor','elevator','raytracer','montecarlo','hedc','jspider','weblech','ftp']

settings=['oracle', 'dynamic']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaCipaPts.py '+bench + ' ' +setting + ' '+code
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True) 
        process.wait()
        print process.returncode
