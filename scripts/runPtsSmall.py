import subprocess

benches=['sor','elevator','raytracer','montecarlo']

settings=['oracle', 'dynamic']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaCipaPts.py '+bench + ' ' +setting + ' 12'
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE) 
        process.wait()
        print process.returncode
