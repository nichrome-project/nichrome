import subprocess
from matplotlib import pyplot as plt
from matplotlib.ticker import MaxNLocator
import numpy as np

def setMaxTicks(i):
    plt.gca().xaxis.set_major_locator( MaxNLocator(nbins = i, prune = 'lower') )
    plt.gca().yaxis.set_major_locator( MaxNLocator(nbins = i+1) )

def execute(cmd):
    print 'Executing: '+cmd
    subprocess.call(cmd,shell=True)

def getMainPlotName(client):
    return client+'_main.pdf'

def getSensiviityPlotName(client):
    return client+'_sens.pdf'

def getSensivitiyIterPlotName(client,bench):
    return client+'_'+bench+'_sens_iter.pdf'

def getMainFileName(client):
    return client+'_main.txt'

def getIterFileName(bench,config,client):
    return client+'_'+bench+'_'+config+'_iter'+'.txt'

def getIterFilePlotName(client,bench):
    return client+'_'+bench+'_iter'+'.pdf'

def getIterGridPlotFileName(client):
    return client+'_iter.pdf'

def getIterTimeFileName(client,config,bench):
    return client+'_'+bench+'_'+config+'_itertime'+'.txt'

def getIterTimePlotFileName(client):
    return client+'_itertime'+'.pdf' 

def getSolverTimeFileName(bench,config,client):
    return client+'_'+bench+'_'+config+'_solvertime'+'.txt'

def getSolverTimePlotFileName(client):
    return client+'_solvertime.pdf'

def fileToIntMatrix(fpath, sep = ' '):
    ret = list()
    with open(fpath) as f:
        for line in f.readlines():
            curItem = list()
            line = line.strip()
            tokens = line.split(sep)
            for t in tokens:
                curItem.append(int(t))
            ret.append(curItem)
    return ret

def fileToStrMatrix(fpath, sep = ' '):
    ret = list()
    with open(fpath) as f:
        for line in f.readlines():
            curItem = list()
            line = line.strip()
            tokens = line.split(sep)
            for t in tokens:
                curItem.append(str(t))
            ret.append(curItem)
    return ret

