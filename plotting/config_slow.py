benchmarks = ['raytracer','montecarlo', 'sor', 'elevator', 'jspider', 'ftp', 'hedc', 'weblech']

raceConfigs = ['staticAggr', 'staticCons', 'dynamic', 'bagged', 'oracle', 'uniform']

ptsConfigs = ['dynamic','oracle']

runFolder = '../main/ursa_bench_slow'

dataFolder = '../data_slow'

plotFolder = '../plots_slow'

backupFolder = '../backup_slow'

classifiers = ['oracle', 'bagged', 'dynamic', 'staticCons', 'staticAggr']

raceExprMachineMap = {'oracle':'fir04', 'dynamic':'fir05', 'staticAggr':'fir07', 'staticCons':'fir06', 'bagged':'fir08'}

raceBaselineMachineMap = {'raytracer':'fir08','montecarlo':'fir07','sor':'fir05','elevator':'fir05', 'jspider':'fir08', 'ftp':'fir07', 'hedc':'fir06', 'weblech':'fir06'}

config_label_map = {'oracle':'ideal', 'bagged':'aggregated', 'dynamic':'dynamic', 'staticCons':'static_optimistic', 'staticAggr':'static_pessimistic', 'uniform':'baseline'}

ptsExprMachineMap = {
        ('raytracer','oracle'):'fir03', 
        ('raytracer','dynamic'):'fir03', 
        ('elevator','oracle'): 'fir03', 
        ('elevator','dynamic'):'fir03',
        ('jspider','oracle'):'fir11',
        ('jspider','dynamic'):'fir11',
        ('ftp','oracle'):'fir13',
        ('ftp','dynamic'):'fir08',
        ('hedc','oracle'):'fir06',
        ('hedc','dynamic'):'fir03',
        ('weblech','oracle'):'fir12',
        ('weblech','dynamic'):'fir04',
        ('montecarlo','oracle'):'fir03',
        ('montecarlo','dynamic'):'fir03',
        ('sor','oracle'):'fir03',
        ('sor','dynamic'):'fir03',      
        }

def getRaceExprLocs():
    ret = {}
    for config, machine in raceExprMachineMap.iteritems():
        for bench in benchmarks:
            ret[(bench,config)] = machine
    for bench in benchmarks:
        ret[(bench,'uniform')] = raceBaselineMachineMap[bench]
    return ret

def getPtsExprLocs():
    return ptsExprMachineMap

def configToLabel(config):
    return config_label_map[config]
