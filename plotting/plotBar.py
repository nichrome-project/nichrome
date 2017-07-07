#!/usr/bin/python

import sys
import numpy as np
import math
import matplotlib
import matplotlib.pyplot as plt
import itertools
import os
from matplotlib import rcParams
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


#def flip(items, ncol):
#     return itertools.chain(*[items[i::ncol] for i in range(ncol)])

def divideList(l1, l2):
    ret = []
    for v1, v2 in zip(l1,l2):
        if v2 == 0:
            ret.append(None)
        else:
            ret.append(float(v1)/v2)
    return ret

def average(arr):
    arrSum = 0
    length = 0
    for v in arr:
        if v != None:
            length += 1
            arrSum += v
    return arrSum/length

def column(matrix, i):
        return [row[i] for row in matrix]

def to_percent(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = '{:.0f}'.format(100 * y)

    # The percent symbol needs escaping in latex
    if matplotlib.rcParams['text.usetex'] == True:
        return s + r'$\%$'
    else:
        return s + '%'

def autolabel(ax, rects, labels):
    # attach some text labels
    for rect, l in zip(rects,labels):
        height = rect.get_height()
        ax.text(rect.get_x()+rect.get_width()/2., 1.05*height, str(l)+' ',
                ha='center', va='bottom', fontsize=11)

def plotBars(client, show_legend, plotAll, plotBaseline=False ,bar_width = 0.2, cluster_space = 0.4, xo = 0.2, xmax = -1, xlabel_rotate_angle = 0, ymin = 0, ymax =1, aspect_ratio=0.678, leg_rows=1, show_avg = False): 
    print 'IfAll '+str(plotAll)
    print client

    data_file_path = os.path.join(dataFolder, myUtils.getMainFileName(client))
    matrix = myUtils.fileToStrMatrix(data_file_path)

    qcolors = [0.8, 0.6, 0.4, 0.2, 0]
    rcolors = [1, 1, 1, 1, 1] 

    local_config_label_map = dict(config_label_map)

    if plotBaseline and not plotAll:
        print 'Must set plotAll to True to plot baseline'
        exit(1)

    if not plotBaseline and client == 'datarace':
        matrix = [r[:-2] for r in matrix]

    if not plotAll:
        matrix = [r[0:2]+r[-4:] for r in matrix]
        config_label_map['bagged']='URSA'
        config_label_map['dynamic'] = 'URSA'

    r0 = matrix[0][2:]

    legends = []

    for n in r0[0::2]:
        legends.append(configToLabel(n.split('-')[0]))
    
    if len(legends) == 2:
        qcolors = [qcolors[0],qcolors[-1]]
        rcolors = [rcolors[0],rcolors[-1]]

    benches = [r[0] for r in matrix[1:]]

    if show_avg:
        benches.append('avg.')

    numConfig = len(legends)

    data = [r[1:] for r in matrix[1:]]

    totals = [int(r[0]) for r in data]

    data = [r[1:] for r in data]

    # config -> questions[bench]
    questions = []
    # config -> reports[bench]
    reports = []
    for i in range(numConfig):
        curQs = []
        curRs = []
        for r in data:
            curQs.append(int(r[i*2]))
            curRs.append(int(r[i*2+1]))
        questions.append(curQs)
        reports.append(curRs)

    num_clusters = len(benches)

    ind = np.arange(num_clusters)

    colors = []

    num_legend_groups = len(legends)

    group_width = num_legend_groups*bar_width+cluster_space
    
    fig = plt.figure()

    ax = fig.add_subplot(1,1,1)

    questions_rate = []
    for qs in questions:
        curItem = divideList(qs, totals)
        if show_avg:
            curItem.append(average(curItem))
        curItem = [ v if v!= None else 0 for v in curItem]
        questions_rate.append(curItem)

    reports_rate = []
    for rs in reports:
        curItem = divideList(rs,totals)
        if show_avg:
            curItem.append(average(curItem))
        curItem = [ v if v!= None else 0 for v in curItem]
        reports_rate.append(curItem)

    print benches
    print questions_rate
    print reports_rate


    ax.yaxis.grid(True, linewidth = 1)
    ax.set_axisbelow(True)

    for i in range(0, len(questions_rate)):
    # plot reports
        single_data_set = reports_rate[i]
        r_rects = ax.bar(ind*group_width+i*bar_width+xo, single_data_set, bar_width, color=str(rcolors[i])) 
 
    # plot questions
        single_data_set = questions_rate[i]
        q_rects = ax.bar(ind*group_width+i*bar_width+xo, single_data_set, bar_width, color=str(qcolors[i]), label=legends[i], alpha= 1)
        if not plotAll:
            if show_avg:
                autolabel(ax,q_rects[:-1], questions[i])
            else:
                autolabel(ax,q_rects, questions[i])          

        for q_rect, r_rect in zip(q_rects, r_rects):
            if r_rect.get_height() <= q_rect.get_height():
                y = r_rect.get_height()
                if y == 0 or y == q_rect.get_height():
                    y+= 0.002
                if y >= 1:
                    y = 1- 0.002
                ax.plot([r_rect.get_x(), r_rect.get_x() + r_rect.get_width()],[y,y], linewidth=1.5, linestyle='-', color='black')

    formatter = FuncFormatter(to_percent)
    plt.gca().yaxis.set_major_formatter(formatter)

    xtick_pos = ind*group_width+num_legend_groups*bar_width/2+xo

    # set the x sticks
    if xlabel_rotate_angle != 0:
        ax.set_xticks(xtick_pos)
        ax.set_xticklabels(tuple(benches), rotation = xlabel_rotate_angle)
        for tick in ax.xaxis.get_major_ticks():
                tick.label1.set_horizontalalignment('right')
    else:
        ax.set_xticks(xtick_pos)
        ax.set_xticklabels(tuple(benches))

    # set the aspect and the range of y
    
    x1,x2,y1,y2 = plt.axis()
    
    if ymin > 0:
        y1 = ymin
    
    if ymax > 0:
        y2 = ymax
    
    ylength = y2-y1
    
    xlength = x2-x1

    if xmax > 0:
        x2 = xmax
    else:
        x2 = xo *2 + num_legend_groups*bar_width*num_clusters + cluster_space*(num_clusters-1)

    ax.set_ylim([y1,y2])
    ax.set_xlim([x1,x2])
    
    aspect = float(xlength)/ylength * aspect_ratio 
    
    ax.set_aspect(aspect)

    # annotate the number of alarms

    if plotAll:
        fig.text(1, 0.73, '$\leftarrow$#  false\n   alarms', ha='center', va='center')
    else: 
        fig.text(1, 0.78, '$\leftarrow$#  false\n   alarms', ha='center', va='center')

    for pos, t in zip(xtick_pos, totals):
        ax.text(pos, 1.02*y2, '%d' % t,   ha='center', va='bottom', fontsize=12)

    # put titles on x and y
    ax.set_ylabel('causes and alarms', fontweight='normal')

    # line separate avg.

    line_pose = (xtick_pos[-1]+xtick_pos[-2])/2

    if show_avg:
        ax.plot([line_pose,line_pose],[0,1], linestyle='--', color = '0')

    if show_legend:
        num_col = int(math.ceil(float(num_legend_groups)/leg_rows))
        handles, labels = ax.get_legend_handles_labels()
        ypos = 1.1+0.03*(leg_rows-1)
        if aspect_ratio < 0.5:
            ypos = 1.1+0.05*(leg_rows-1)
        fontsize = 16
        if num_col > 2:
            fontsize = 14
        plt.legend(bbox_to_anchor=(0., ypos, 1., .11), loc=10, prop={'size':fontsize, 'weight':'normal'}, ncol=num_col, mode='expand', borderaxespad=0., frameon=True)

    if plotAll:
        plotName = myUtils.getSensiviityPlotName(client)
    else:
        plotName = myUtils.getMainPlotName(client)
    plotPath = os.path.join(plotFolder, plotName)
    plt.savefig(plotPath, bbox_inches='tight')
    # recover
    for k, v in local_config_label_map.iteritems():
        config_label_map[k] = v

# main result
plotBars('datarace', True, False, bar_width=0.8, cluster_space=0.5, xlabel_rotate_angle =37, aspect_ratio = 0.5)
plotBars('pts', True, False, bar_width=0.8, cluster_space=0.5, xlabel_rotate_angle =37, aspect_ratio = 0.5)

# sensivity
plotBars('datarace', True, True, bar_width=0.4, cluster_space=0.8, xlabel_rotate_angle =37, aspect_ratio = 0.4, leg_rows = 2)


