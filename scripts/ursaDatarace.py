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

classifierMap = {'oracle':'craftedCons', 'dynamic':'dynamic', 'staticAggr':'craftedAggr', 'staticCons':'craftedCons', 'bagged':'bag', 'uniform':'uniform'}

pjbench = os.environ['PJBENCH']
bench_paths = {}
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
bench_paths['schroeder-m'] = pjbench + '/ashesJSuite/benchmarks/schroeder-m/'

bench_name = sys.argv[1]
bench_path = bench_paths[bench_name]

classifier = sys.argv[2]
percent = 100
if argc >=5:
	percent = int(sys.argv[4])

def getRelName(line):
    line = line.strip()
    open_b_ind = line.index('(')
    close_b_ind = line.index(')')
    rel_name = line[0:open_b_ind]
    return rel_name


def idToDesc(e_orig_file,proven_queries_file,proven_queries_mapped_file):
	domE = []
	e_orig = open(e_orig_file,'r')
	for line in e_orig.readlines():
		line = line.strip()
		domE.append(line)

	e_orig.close()

	proven_queries = open(proven_queries_file,'r')
	notThrEsc = []
	for line in proven_queries.readlines():
		line = line.strip()
		notThrEsc.append(int(line))
	proven_queries.close()

	proven_queries_mapped = open(proven_queries_mapped_file,'w')
	for iD in notThrEsc:
		proven_queries_mapped.write(str(domE[iD])+'\n')

	proven_queries_mapped.close()


def DescToId(e_new_file,proven_queries_mapped_file,proven_queries_ids_file):
	domE = []
	e_new = open(e_new_file,'r')
	for line in e_orig.readlines():
		line = line.strip()
		domE.append(line)

	e_new.close()

	proven_queries_mapped = open(proven_queries_mapped_file,'r')
	notThrEsc = []
	for line in proven_queries.readlines():
		line = line.strip()
		notThrEsc.append(int(line))
	proven_queries_mapped.close()

	proven_queries_ids = open(proven_queries_ids_file,'w')
	for iD in notThrEsc:
		proven_queries_ids.write(str(domE[iD])+'\n')

	proven_queries_ids.close()


def waitForJava():
	return

def enterStep(opt):
	print 'Entering '+opt

def exitStep(opt):
	print 'Exiting '+opt

mlnInfDir = os.environ['NICHROME_MAIN']+'/'
labelDir = os.environ['URSA_LABELS']+'/'
homeDir = os.environ['HOME']+'/'
chordMainDir = os.environ['CHORD_MAIN'] + '/'
analysisRunPath = bench_path+'chord_output_ursa-datarace_'+classifier+'/'
preciseRels = ['MPtail','threadStartI','statF','EV','EF','PE','MPhead','MmethArg','PP','PI']
preciseRels = set(preciseRels)
questionRels = ['escE','CICM','PathEdge_cs', 'racePairs_cs']
questionRels = set(questionRels)

labelFile = os.path.join(labelDir,'race_'+bench_name+".txt")

if usePre:
    mlnInfBenchDir = os.path.join(mlnInfDir, 'ursa_bench', bench_name+'-datarace_'+classifier+'/')
else:
    mlnInfBenchDir = os.path.join(mlnInfDir, 'ursa_bench_slow', bench_name+'-datarace_'+classifier+'/')

if not os.path.exists(mlnInfBenchDir) :
	os.makedirs(mlnInfBenchDir)


step = int(sys.argv[3])

if step==1 or step == 12:
	enterStep('Input generation')
	runner_pl = os.path.join( chordMainDir, 'runner.pl' )
	cmdOracle = runner_pl +' -mode=serial -program='+bench_name+' -analysis=ursa-datarace -D chord.out.dir=chord_output_ursa-datarace_'+classifier
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
	f2.write('infi: PathEdge_cs(0,0,1,0,0)\n')
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
		print 'Error copying label_derived.edb'
		print status,output

	f1 = open(analysisRunPath+'/label_input.edb','r')
        f2 = open(mlnInfBenchDir+'/feedback.edb','a')
	for line in f1.readlines():
		line = line.strip()
		f2.write(line+'\n')
        f1.flush()
        f2.flush()
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
	f1.close()
        f2.close()


	candidateList = []
	f1 = open(mlnInfBenchDir+'/base_scope.edb','r')
	for line in f1.readlines():
		line = line.strip()
		#if ('escE' in line):#  or ('racePairs_cs' in line) :
		candidateList.append(line)
	f1.close()
	f1 = open(mlnInfBenchDir+'/problem.edb','r')
	for line in f1.readlines():
		line = line.strip()
		#if ('escE' in line):#  or ('racePairs_cs' in line) :
		candidateList.append(line)
	f1.close()

	f1 = open(mlnInfBenchDir+'/problem.edb','r')
	f2 = open(mlnInfBenchDir+'/ursa_prelabelled.txt','w')
	for line in f1.readlines():
		if line.startswith('//'):
			continue
		if line == "\n":
			continue
		line = line.strip()
		rel_name = getRelName(line)
		if rel_name in preciseRels:
			f2.write(line+' T\n')
	f1.close()
	f2.close()
        cmd = 'cp '+analysisRunPath+'/bddbddb/OrderedEE.bdd '+mlnInfBenchDir
        (status,output) = commands.getstatusoutput(cmd)
        if status != 0:
            print 'Error copying OrderedEE.bdd to mlninference'
            print status,output
            exit(1)
        cmd = 'cp '+analysisRunPath+'/bddbddb/E.dom '+mlnInfBenchDir
        (status,output) = commands.getstatusoutput(cmd)
        if status != 0:
            print 'Error copying OrderedEE.bdd to mlninference'
            print status,output
            exit(1)
        cmd = 'cp '+analysisRunPath+'/correlEE.txt '+mlnInfBenchDir
        (status,output) = commands.getstatusoutput(cmd)
        if status != 0:
            print 'Error copying OrderedEE.bdd to mlninference'
            print status,output
            exit(1)

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

        appQFile = os.path.join(analysisRunPath,'app_base_scope.edb')
        appQFileDst = os.path.join(mlnInfBenchDir,'app_base_scope.edb')
        cmd = 'cp '+appQFile+' '+appQFileDst
	(status,output) = commands.getstatusoutput(cmd)
	if(status != 0):
		print 'Error copying app_base_scope.edb'
		print status,output

	exitStep('Input generation')

if step == 2 or step == 12:
	enterStep('URSA main loop')

	eugene_jar = os.path.join(mlnInfDir, 'nichrome.jar')
	eugene_conf = os.path.join(mlnInfDir, 'Nichrome.conf')
	mln_src = os.path.join(mlnInfDir, 'src')
	mln_files = os.path.join(chordMainDir, 'src/chord/analyses/ursa/datarace/datarace.mln')

	ns = [1000]
	for num in ns:
		for i in range(1,2):
			cmd  = ' nohup java -Xmx64g'
			cmd += ' -Dchord.main.dir=' + chordMainDir
			cmd += ' -Dchord.work.dir=' + mlnInfBenchDir
			cmd += ' -Dchord.out.dir=' + mlnInfBenchDir
			cmd += ' -Dchord.bddbddb.work.dir=' + mlnInfBenchDir
			cmd += ' -Dchord.dlog.analysis.path=' + mln_src
			cmd += ' -Dchord.java.analysis.path=' + eugene_jar
			cmd += ' -Dchord.reuse.rels=true -Dchord.run.analyses=thresc-dlog -Dchord.print.rels=escEDep,localEDep '
			cmd += ' -jar ' + eugene_jar
                        cmd += ' MLN'
			cmd += ' -conf ' + eugene_conf
			cmd += ' -e '+ os.path.join(mlnInfBenchDir, 'problem.edb')
			cmd += ' -r '+ os.path.join(mlnInfBenchDir, 'result_'+ str(num) +'_'+str(i)+'.txt')
			cmd += ' -i ' + mln_files
			cmd += ' -loadgc '+ os.path.join(mlnInfBenchDir, 'cons_all.txt')
			cmd += ' -verbose 2 -ignoreWarmGCWeight -alarmResolution -printVio -numFeedback 1'
			cmd += ' -queryFile ' + os.path.join(mlnInfBenchDir, 'base_queries.txt')
			cmd += ' -oracleTuplesFile ' + os.path.join(mlnInfBenchDir, 'feedback.edb')
			cmd += ' -ursaSearchSpace ' + os.path.join(mlnInfBenchDir, 'ursa_search_space.txt')
			cmd += ' -ursaPreLabelledTuples ' + os.path.join(mlnInfBenchDir, 'ursa_prelabelled.txt')
			cmd += ' -pickBudget '+str(num)
			cmd += ' -ursaClient datarace'
                        cmd += ' -picker 0'
                        cmd += ' -appQuestions '+os.path.join(mlnInfBenchDir,"app_base_scope.edb")

                        if not usePre:
                            cmd += ' -offPre'

                        if classifier != 'oracle':
                            cmd += ' -ursaExternalModel '+os.path.join(mlnInfBenchDir, 'prediction.txt')
			cmd += ' > ' + os.path.join(mlnInfBenchDir, 'log_ursa_datarace_'+classifier+'_'+ str(num) +'_'+str(i)+'.txt ')


			print 'Running ',cmd
			(status,output) = commands.getstatusoutput(cmd)
			if status != 0:
				print 'Error running mlninference'
				print status,output
				exit(1)

	exitStep('URSA main loop')



