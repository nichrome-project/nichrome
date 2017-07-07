import subprocess

benches=['sor','elevator','raytracer','montecarlo']

settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting + ' 12 1'
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True) 
        process.wait()
        print process.returncode
