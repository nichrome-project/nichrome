import subprocess

benches=['sor','elevator','raytracer','montecarlo','hedc','jspider','weblech','ftp']

settings=['bagged', 'oracle', 'dynamic', 'staticAggr', 'staticCons']

for bench in benches:
    for setting in settings:
        cmd = 'python ./ursaDatarace.py '+bench + ' ' +setting + ' 12 1'
        print 'Running '+cmd
        process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE) 
        process.wait()
        print process.returncode
