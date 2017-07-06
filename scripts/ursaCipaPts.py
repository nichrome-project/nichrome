import sys
import os
import commands
import time
import math
import random

usage = 'Usage : python ursaDatarace.py <bench_name> <classifer> [step] [turn off preprocessing]'

argc = len(sys.argv)

if argc < 4:
	print usage
	exit(1)

usePre = True

if argc == 5:
    print 'Warning: preprocessing turned off. Expected to take more time.'
    usePre = False

classifierMap = {'oracle':'none', 'dynamic':'dynamic'}

pjbench = os.environ['PJBENCH']
mlnInfDir = os.environ['NICHROME_MAIN']+'/'
homeDir = os.environ['HOME']+'/'
labelDir = os.environ['URSA_LABELS']+'/'
chordMainDir = os.environ['CHORD_MAIN'] + '/'
mlnFileDir = os.path.join(chordMainDir, "src/chord/analyses/ursa/cipa")

bench_paths = {}

bench_paths['test'] = chordMainDir+"../test/tests/alias1/"

bench_paths['sor'] = pjbench+'/sor/'
bench_paths['raytracer'] = pjbench+'/java_grande/raytracer/'
bench_paths['montecarlo'] = pjbench+'/java_grande/montecarlo/'
bench_paths['philo'] = pjbench+'/java_grande/philo/'
bench_paths['shop'] = pjbench+'/contest/shop/'
bench_paths['pingpong'] = pjbench+'/contest/pingpong/'
bench_paths['mergesort'] = pjbench+'/contest/mergesort/'
bench_paths['tsp'] = pjbench+'/tsp/'
bench_paths['elevator'] = pjbench+'/elevator/'
bench_paths['hedc'] = pjbench+'/hedc/'
bench_paths['ftp'] = pjbench+'/ftp/'
bench_paths['weblech'] = pjbench+'/weblech-0.0.3/'
bench_paths['jspider'] = pjbench+'/jspider/'
bench_paths['dracetest'] = pjbench+'/drace_test/'
bench_paths['batik'] = pjbench+'/dacapo/benchmarks/batik/'
bench_paths['antlr'] = pjbench+'/dacapo/benchmarks/antlr/'
bench_paths['avrora'] = pjbench+'/dacapo/benchmarks/avrora/'
bench_paths['antlr'] = pjbench+'/dacapo/benchmarks/antlr/'
bench_paths['bloat'] = pjbench+'/dacapo/benchmarks/bloat/'
bench_paths['chart'] = pjbench+'/dacapo/benchmarks/chart/'
bench_paths['fop'] = pjbench+'/dacapo/benchmarks/fop/'
bench_paths['luindex'] = pjbench+'/dacapo/benchmarks/luindex/'
bench_paths['lusearch'] = pjbench+'/dacapo/benchmarks/lusearch/'
bench_paths['pmd'] = pjbench+'/dacapo/benchmarks/pmd/'
bench_paths['sunflow'] = pjbench+'/dacapo/benchmarks/sunflow/'
bench_paths['xalan'] = pjbench+'/dacapo/benchmarks/xalan/'
bench_paths['hsqldb'] = pjbench+'/dacapo/benchmarks/hsqldb/'


bench_name = sys.argv[1]
bench_path = bench_paths[bench_name]

classifier = sys.argv[2]
percent = 100

def getRelName(line):
    line = line.strip()
    open_b_ind = line.index('(')
    close_b_ind = line.index(')')
    rel_name = line[0:open_b_ind]
    return rel_name


def waitForJava():
	return

def enterStep(opt):
	print 'Entering '+opt

def exitStep(opt):
	print 'Exiting '+opt

if argc >= 4:
	step = int(sys.argv[3])

# Book keeping step
# 	Create corresponding directory in mlninference/mln_bench
#	map the proven queries and E.map and write the resulting file into this new directory

if usePre:
    mlnInfBenchDir = mlnInfDir+'/ursa_bench/'+bench_name+'-pts_'+classifier+'/'
else:
    mlnInfBenchDir = mlnInfDir+'/ursa_bench_slow/'+bench_name+'-pts_'+classifier+'/'

if not os.path.exists(mlnInfBenchDir) :
	os.makedirs(mlnInfBenchDir)
   
step = int(sys.argv[3])

analysisRunPath = bench_path+'chord_output_ursa-cipa-pts_'+classifier+'/'

preciseRels = ['VT', 'HT', 'cha', 'sub', 'MmethArg', 'MmethRet', 'IinvkArg0', 'IinvkArg', 'IinvkRet', 'MI', 'statIM', 'specIM', 'virtIM', 'MobjValAsgnInst', 
'MobjVarAsgnInst', 'MgetInstFldInst', 'MputInstFldInst', 'MgetStatFldInst', 'MputStatFldInst', 'clsForNameIT', 'objNewInstIH', 'objNewInstIM', 'conNewInstIH', 'conNewInstIM',
'aryNewInstIH', 'classT', 'staticTM', 'staticTF', 'clinitTM', 'checkExcludedH', 'checkExcludedV', 'MV', 'MH', 'VHfilter']
preciseRels = set(preciseRels)
questionRels = ['VH','IM']
questionRels = set(questionRels)

labelFile = os.path.join(labelDir,'cipa_'+bench_name+".txt")

if step==1 or step == 12:
	enterStep('Input generation')

	runner_pl = os.path.join( chordMainDir, 'runner.pl' )
	cmdOracle = runner_pl + ' -mode=serial -program='+bench_name+' -analysis=ursa-cipa -D chord.out.dir='+analysisRunPath
        cmdOracle += ' -D chord.ursa.classifierFile='+os.path.abspath('./trained.txt')
        cmdOracle += (' -D chord.ursa.classifier='+classifierMap[classifier])
        cmdOracle += ' -D chord.ursa.labelFile='+labelFile

	print 'Running ',cmdOracle
	(status,output) = commands.getstatusoutput(cmdOracle)
	if status != 0:
		print 'Error running Oracle run'
		print status,output
		exit(1)
	waitForJava()

	orig_cons_all = analysisRunPath+'/cons_all.txt'
	new_cons_all = mlnInfBenchDir+'/cons_all.txt'
	f1 = open(orig_cons_all,'r')
	f2 = open(new_cons_all,'w')
	f2.write('infi: rootM(0)\n')
        f2.write('infi: reachableM(0)\n')
	for line in f1.readlines():
            f2.write(line)
	f1.close()
	f2.close()

	cmd = 'cp '+analysisRunPath+'/base_queries.txt '+mlnInfBenchDir+'/'
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying base queries.txt'
		print status,output

	cmd = 'cp '+analysisRunPath+'/base_scope.edb '+mlnInfBenchDir+'/base_scope.edb'
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying base_scope.edb'
		print status,output

	cmd = 'cp '+analysisRunPath+'/base_problem.edb '+mlnInfBenchDir+'/problem.edb'
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying problem.edb'
		print status,output

	cmd = 'cp '+analysisRunPath+'/label_derived.edb '+mlnInfBenchDir+'/feedback.edb'
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying feedback.edb'
		print status,output
	
	f1 = open(analysisRunPath+'/label_input.edb','r')
        f2 = open(mlnInfBenchDir+'/feedback.edb','a')
	for line in f1.readlines():
		line = line.strip()
		f2.write(line+'\n')
	f1.close()
	f2.close()

        f1 = open(mlnInfBenchDir+'/feedback.edb')
        f2 = open(mlnInfBenchDir+'/ursa_search_space.txt','w')
	for line in f1.readlines():
		line = line.strip()
                if(line.startswith('//')):
                    continue
                if(line.startswith('!')):
                    line = line[1:]
                rel_name = getRelName(line)
                if(rel_name in questionRels):
                    f2.write(line+"\n")
        f2.flush()
	f2.close()
        f1.close()

	f1 = open(mlnInfBenchDir+'/problem.edb','r')
	f2 = open(mlnInfBenchDir+'/ursa_prelabelled.txt','w')
	for line in f1.readlines():
		if line.startswith('//'):
			continue
		if line == "\n":
			continue
		line = line.strip()
		open_b_ind = line.index('(')
		close_b_ind = line.index(')')
		rel_name = line[0:open_b_ind]
		if rel_name in preciseRels:
			f2.write(line+' T\n')
	f1.close()
	f2.close()

        predFile = os.path.join(mlnInfBenchDir,'prediction.txt')
        cmd = 'echo 0.51 > '+predFile
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying prediction.txt'
		print status,output

	cmd = 'cat '+analysisRunPath+'/prediction.txt >> '+predFile
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying prediction.txt'
		print status,output



	exitStep('Input generation')

if step == 2 or step == 12:
	enterStep('URSA main loop')

	eugene_jar = os.path.join(mlnInfDir, 'nichrome.jar')
	eugene_conf = os.path.join(mlnInfDir, 'Nichrome.conf')
	mln_src = os.path.join(mlnInfDir, 'src')
	mln_files = os.path.join(mlnFileDir, "cipa-0cfa-dlog.mln")
	mln_files += ',' + os.path.join(mlnFileDir, 'cipa-pts-dlog.mln')

	ns = [1000]
	for num in ns:
		for i in range(1,2):
			cmd  = ' nohup java -Xmx64g'
			cmd += ' -jar ' + eugene_jar
                        cmd += ' MLN'
			cmd += ' -conf ' + eugene_conf
			cmd += ' -e '+mlnInfBenchDir+'problem.edb'
			cmd += ' -r '+mlnInfBenchDir+'result_'+ str(num) +'_'+str(i)+'.txt'
			cmd += ' -i ' + mln_files
			cmd += ' -loadgc '+mlnInfBenchDir+'cons_all.txt -verbose 2 -ignoreWarmGCWeight -alarmResolution -printVio -numFeedback 1'
			cmd += ' -queryFile '+mlnInfBenchDir+'base_queries.txt'
			cmd += ' -oracleTuplesFile '+mlnInfBenchDir+'/feedback.edb'
			cmd += ' -ursaSearchSpace '+mlnInfBenchDir+'/ursa_search_space.txt'
			cmd += ' -ursaPreLabelledTuples '+mlnInfBenchDir+'/ursa_prelabelled.txt'
                        cmd += ' -pickBudget '+str(num)
                        cmd += ' -ursaClient pts'
                        cmd += ' -picker 0'
                        if not usePre:
                            cmd += ' -offPre'

                        if classifier != 'oracle':
                            cmd += ' -ursaExternalModel '+os.path.join(mlnInfBenchDir, 'prediction.txt')
			cmd += ' > '+mlnInfBenchDir+'log_ursa_pts_'+classifier+'_'+ str(num) +'_'+str(i)+'.txt '
			print 'Running ',cmd
			(status,output) = commands.getstatusoutput(cmd)
			if status!=0:
				print 'Error runnign MLNInference'
				print status,output

	exitStep('URSA main loop')



