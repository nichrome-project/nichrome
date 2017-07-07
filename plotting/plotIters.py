import sys
import os
import matplotlib.pyplot as plt
import matplotlib
import myUtils
from config import *

legend_bench = 'raytracer'

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
font = {'family' : 'sans-serif',
#        'weight' : 'bold',
        'size'   : 32}

plt.rc('font', **font)

plt.rc('font', **font)
plt.rcParams['xtick.direction'] = 'out'
plt.rcParams['ytick.direction'] = 'out'

w = 0
r = 2

colors = ['red', 'blue', 'green', 'brown', 'pink', 'black']
linestyles = ['-', '-', '-', '-', '-', '-']
markers = ['s', 'x', '^', 'v', '|', 'o']
 

usage = 'Usage: '+sys.argv[0] + ' windowSize rate'

def normalize(i):
    if i < 5:
        return 5
    if i >=1 and i < 30:
        return int(i)/5*5+5
    if i >= 30 and i < 60:
       return 50
    if i >= 60 and i< 75:
        return 75
    if i >=75 and i < 100:
        return 100
    if i > 100 and i < 125:
        return 125
    if i>= 125 and i < 500:
        return int(i)/50*50+50
    if i >= 500 and i < 750:
        return 750
    if i >= 750 and i < 1000:
        return 1000
    if i >= 1000 and i < 1250:
        return 1250
    if i > 3000 and i < 5000:
        return 5000
    return i

if len(sys.argv) != 3:
    print usage
    exit(1)

w = int(sys.argv[1])
r = float(sys.argv[2])

def getDataPoints(client, bench, classi):
    fpath = os.path.join(dataFolder, myUtils.getIterFileName(bench, classi, client))
    if not os.path.isfile(fpath):
        print 'Cannot find '+fpath
        return None
    matrix = myUtils.fileToIntMatrix(fpath)
    xpoints = [0]
    ypoints = [0]
    xwindow = list()
    ywindow = list()
    for row in matrix:
        x = row[0]
        y = row[1]
        if len(xwindow) == w:
            if y - ywindow[0] < (x - xwindow[0])*r:
                break;
            else:
                xwindow = xwindow[1:]
                ywindow = ywindow[1:]

        xwindow.append(x)
        ywindow.append(y)
        xpoints.append(x)
        ypoints.append(y) 

    return (xpoints, ypoints)


def doPlot(client, configs, aspect_ratio=1):
    fig = plt.figure()
    myUtils.setMaxTicks(4)
    for bench in benchmarks:
        f, ax = plt.subplots(1)
        i = 0
        pPath = os.path.join(plotFolder,myUtils.getIterFilePlotName(client,bench))

        maxX = 0
        maxY = 0
        for classi in configs:
            r = getDataPoints(client,bench,classi) 
            if r is None:
                continue
            (x,y) = r
            ax.plot(x,y, color = colors[i], linestyle=linestyles[i], linewidth=4, alpha = 0.8)
            if markers[i] == 'x':
                msize = 200
            else:
                msize = 200
            ax.scatter(x,y, s=msize, color = colors[i], marker=markers[i], label=configToLabel(classi),linewidth=4, alpha = 0.8, facecolors='none')
            i+=1
            maxX = max(maxX, max(x))
            maxY = max(maxY, max(y))
       
        if i == 0:
            continue
    
 #       lgd = plt.legend(bbox_to_anchor=(0., 1.02, 1., .102), loc=3,
 #               ncol=2, mode="expand", borderaxespad=0.)
        if legend_bench == bench:
            lgd = plt.legend(loc=4, fontsize =30)
        x1,x2,y1,y2 = plt.axis()
        
        x1 = 0
        y1 = 0

        if y2 < 1:
            y2 =1 
        if x2 < 1:
            x2 = 1
        x2 = normalize(maxX)
        y2 = normalize(maxY)

        ylength = y2-y1
        
        xlength = x2-x1
    
        ax.set_ylim([y1,y2])
        ax.set_xlim([x1,x2])
        
        aspect = float(xlength)/ylength * aspect_ratio 
        
        ax.set_aspect(aspect)

        # put titles on x and y
        ax.set_ylabel('# alarms')

        ax.set_xlabel('# questions')

        # this will prodcue 5 ticks
        myUtils.setMaxTicks(5)

        ax.yaxis.grid(True, linewidth = 2)
        ax.xaxis.grid(True, linewidth = 2)

        ax.text(0.02*x2, 0.87*y2, bench,
                ha='left', va='bottom', fontsize=36, fontweight='bold')

        if bench == 'sor' and client == 'pts':
            ax.text(0.5*x2, 0.5*y2, 'no false alarms',ha='center', va='bottom', fontsize=36)
    
#        plt.savefig(pPath, bbox_extra_artists=(lgd,), bbox_inches='tight')
        plt.savefig(pPath, bbox_inches='tight')
        plt.clf()

config_label_map['bagged'] = 'URSA'
config_label_map['dynamic'] = 'URSA'
doPlot('datarace', ['oracle','bagged'])
doPlot('pts', ['oracle','dynamic'])
