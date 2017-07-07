#!/usr/bin/python

import os
from matplotlib import rcParams
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from config import *
import myUtils
import numpy as np

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
font = {'family' : 'sans-serif',
#        'weight' : 'bold',
        'size'   : 18}

plt.rc('font', **font)

plt.rc('font', **font)
plt.rcParams['xtick.direction'] = 'out'
plt.rcParams['ytick.direction'] = 'out'

def loadTime(client, bench, config):
    ifName = myUtils.getIterTimeFileName(client,config,bench)
    ifPath = os.path.join(dataFolder, ifName)
    ret = []
    with open(ifPath) as f:
        for line in f.readlines():
            ret.append(int(line.strip()))
        f.close()

    return ret

def doPlot(client, ymin =0, ymax=0, aspect_ratio=0.6, xlabel_rotate_angle=0, xmax = 0, useMin = False, useLog = False):
    if client == 'datarace':
        config = 'bagged'
    else:
        config = 'dynamic'
    dataPoints = []
    if useMin:
        divideUnit = 60.0
    else:
        divideUnit = 1
    for bench in benchmarks:
        dataPoints.append([v/divideUnit for v in loadTime(client,bench,config)])
    fig = plt.figure()

    ax = fig.add_subplot(1,1,1)

    ax.boxplot(dataPoints)

    # set the x sticks
    if xlabel_rotate_angle != 0:
        ax.set_xticklabels(tuple(benchmarks), rotation = xlabel_rotate_angle)
        for tick in ax.xaxis.get_major_ticks():
                tick.label1.set_horizontalalignment('right')

    else:
        ax.set_xticklabels(tuple(benchmarks))

    # set the aspect and the range of y

    if useLog:
        ax.set_yscale('log')
    
    x1,x2,y1,y2 = plt.axis()
    
    if ymin > 0:
        y1 = ymin
    
    if ymax > 0:
        y2 = ymax
    
    ylength = y2-y1
    
    xlength = x2-x1

    if xmax > 0:
        x2 = xmax

    ax.set_ylim([y1,y2])
    ax.set_xlim([x1,x2])
    
    aspect = float(xlength)/ylength * aspect_ratio 
    
    ax.set_aspect(aspect)

    # put titles on x and y
    if useMin:
        unit = 'minutes'
    else:
        unit = 'seconds'
    ax.set_ylabel('runtime (in '+unit+')', fontweight='normal')

    plotName = myUtils.getIterTimePlotFileName(client)
    plotPath = os.path.join(plotFolder, plotName)
    plt.savefig(plotPath, bbox_inches='tight')

doPlot('datarace', aspect_ratio= 0.5, xlabel_rotate_angle=30, useMin=False, useLog=False)
doPlot('pts', aspect_ratio= 0.5, xlabel_rotate_angle=30, useMin=False, useLog = False, ymax=100)
