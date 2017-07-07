import myUtils
from config import *
import collections
import os

def getLogFileName(bench,config,client):
    return bench+'_'+config+'.'+client+'_tmp'

def doDownload(client, user_name, exprMachineMap):
    for (bench,config), machine in exprMachineMap.iteritems():
        cmd = 'scp '+user_name+'@'+machine+'.gtisc.gatech.edu:/home/xzhang/mlninference/ursa_bench/'
        cmd += bench+'-'+client+'_'+config+'/log_ursa_'+client+'_'+config+'_1000_1.txt '+dataFolder+'/'+getLogFileName(bench,config,client)
        myUtils.execute(cmd)

def extractTimeInSecs(line):
    content = line.strip().split('[')[1]
    content = content[:-1]
    tokens = content.split(', ')
    secs = 0
    for t in tokens:
        tokens1 = t.split(' ')
        if tokens1[1] == 'min':
            secs += int(tokens1[0]) * 60
        elif tokens1[1] == 'sec':
            secs += int(float(tokens1[0]))
        else:
            print 'Unkown format: '+line
            exit(1)
    return secs


def extractData(client, benchmarks, configs):
    mainData = collections.OrderedDict()
    for bench in benchmarks:
        mainData[bench] = []
        for config in configs:
            logFile = runFolder+'/'+bench+'-'+client+'_'+config+'/log_ursa_'+client+'_'+config+'_1000_1.txt'
            if(not os.path.exists(logFile)):
                print 'Cannot find '+logFile
                print 'Create dummy files instead '
                with open(dataFolder+'/'+myUtils.getIterFileName(bench,config,client),'w') as f:
                    f.write('0 0'+'\n')
                    f.flush()
                    f.close()

                with open(dataFolder+'/'+myUtils.getSolverTimeFileName(bench,config,client),'w') as f:
                    f.write('1\n')
                    f.flush()
                    f.close()

                with open(dataFolder+'/'+myUtils.getIterTimeFileName(client,config,bench), 'w') as f:
                    f.write('1\n')
                    f.flush()
                    f.close()

                if len(mainData[bench]) == 0:
                    mainData[bench].append(1)
                mainData[bench].append((0,0))

                continue
            ifs = open(logFile)
            lines = ifs.readlines();
            i = 0
            
            solver_times = list()

            xpoints = []
            ypoints = []
            solver_times = []
            curSolverTimes = []
            iterTimes = []

            lastTime = -1
            
            while i < len(lines):
                line = lines[i]
                if line.startswith('INFO: time consumed'): 
                    curTime = extractTimeInSecs(line)
                    if lastTime != -1:
                        iterTimes.append(curTime-lastTime)
                    lastTime = curTime
                elif line.startswith('TIMER ilp'):
                    secs = extractTimeInSecs(line)
                    curSolverTimes.append(secs)
                elif line.startswith('After correlation'):
                    solver_times.append(curSolverTimes[:])
                    curSolverTimes = []
                    i+=1
                    line = lines[i]
                    tokens = line.split('Resolved(false):') 
                    rn = tokens[1].strip()
                    ypoints.append(int(rn))
                    i+=2
                    line = lines[i]
                    tokens = line.split('Num of questions')
                    qn = tokens[1].strip()
                    xpoints.append(int(qn))
                elif line.startswith('Ask questions:'):
                    numQues = line.count('), ')
                    gain = int(lines[i-1].split(': ')[1])
                    if gain <= numQues:
                        print 'stop at Line '+str(i) + ' in '+logFile 
                        print lines[i-1].strip()
                        print lines[i]
                        break
                elif line.startswith('Statistics of reports,'):
                    tokens = line.split('false: ')
                    nfqs = int(tokens[1])
                    if len(mainData[bench]) == 0:
                        mainData[bench].append(nfqs)
                    elif mainData[bench][0] <= 1:
                        mainData[bench][0] = nfqs
                i+=1
                # end iterating log file
            ifs.close()

            if len(xpoints) == 0:
                mainData[bench].append((0,0))
            else:
                mainData[bench].append((xpoints[-1],ypoints[-1]))
            
            with open(dataFolder+'/'+myUtils.getIterFileName(bench,config,client),'w') as f:
                for x,y in zip(xpoints, ypoints):
                    f.write(str(x)+' '+str(y)+'\n')
                f.flush()
                f.close()

            with open(dataFolder+'/'+myUtils.getSolverTimeFileName(bench,config,client),'w') as f:
                for times in solver_times:
                    for t in times:
                        f.write(str(t)+' ')
                    f.write('\n')
                f.flush()
                f.close()

            with open(dataFolder+'/'+myUtils.getIterTimeFileName(client,config,bench), 'w') as f:
                for time in iterTimes:
                    f.write(str(time)+'\n')
                f.flush()
                f.close()
            # end iterating each config

    # output the main data
    with open(os.path.join(dataFolder,myUtils.getMainFileName(client)), 'w') as f:
        f.write('benchmarks')
        f.write(' total')
        for c in configs:
            f.write(' '+c+'-questions')
            f.write(' '+c+'-reports')
        f.write('\n')
        for k,v in mainData.iteritems():
            f.write(k)
            f.write(' '+str(v[0]))
            for q,r in v[1:]:
                f.write(' '+str(q))
                f.write(' '+str(r))
            f.write('\n')

        f.flush()
        f.close()

def backupTmpFiles():
    myUtils.execute('mv '+dataFolder+'/*_tmp '+backupFolder)

#download raw logs for datarace

if not os.path.exists(dataFolder):
    os.mkdir(dataFolder)

if not os.path.exists(plotFolder):
    os.mkdir(plotFolder)

#doDownload('datarace', user_name, getRaceExprLocs())
 
extractData('datarace', benchmarks, raceConfigs)

#doDownload('pts', user_name, getPtsExprLocs())

extractData('pts', benchmarks, ptsConfigs)

#backupTmpFiles()
