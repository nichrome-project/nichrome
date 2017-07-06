package nichrome.ursa.maymust;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.MarkovLogicNetwork;

public class AnalysisWithAnnotationsCycle extends AnalysisWithAnnotations {
	private FalseLabelAnalysis fla;
	private Set<Integer> externalFalseLabels;
	private Set<Integer> externalTrueLabels;

	public AnalysisWithAnnotationsCycle(Set<InferenceRule> rules, MarkovLogicNetwork mln) {
		super(rules, mln);
		this.fla = new FalseLabelAnalysis(rules);
		this.externalFalseLabels = new LinkedHashSet<Integer>();
		this.externalTrueLabels = new LinkedHashSet<Integer>();
	}
	
	private boolean isBinaryRule(InferenceRule r){
		return r.getCondition().size() == 1 && r.getCondition().get(0) > 0;
	}
	
	@Override
	public void simplify(Set<Integer> userFalseTuples, Set<Integer> reports) {
		System.out.println("# rules (before simplification): "+rules.size());
		// Partial evaluation
		AnalysisWithAnnotationsCycle dryRunAnalysis = new AnalysisWithAnnotationsCycle(this.rules,mln);
		Set<Pair<Integer,Label>> labels = new HashSet<Pair<Integer,Label>>();
		for(int t : userFalseTuples)
			labels.add(new Pair<Integer,Label>(t, Label.FALSE));
		dryRunAnalysis.labelAndPropagate(labels);
		if (AlarmResolutionDriver.client != null && AlarmResolutionDriver.client.equals("datarace"))
			dryRunAnalysis.postProcess();
		Set<InferenceRule> newRules = new HashSet<InferenceRule>();
		for(InferenceRule r : this.rules){
			int result = r.getResult();
			List<Integer> nCond = new ArrayList<Integer>();
			if(dryRunAnalysis.getLabel(r.getResult()) != Label.FALSE){
				continue;
			}
			for(int c : r.getCondition()){
				Label l = dryRunAnalysis.getLabel(Math.abs(c));
				if(c > 0 && l != Label.FALSE)
					continue;
				nCond.add(c);
			}	
			InferenceRule  nRule = new InferenceRule(result, nCond, r.kind);
			newRules.add(nRule);
		}
		
		System.out.println("# rules (after partial evaluation): "+newRules.size());
		
		// shortcut
		Map<Integer, Set<Integer>> domMap = new HashMap<Integer,Set<Integer>>();
		Map<Integer, Set<Integer>> depMap = new HashMap<Integer,Set<Integer>>();
		Map<Integer, Set<Integer>> revDepMap = new HashMap<Integer,Set<Integer>>();
		Set<Integer> workList = new HashSet<Integer>();
		for(InferenceRule r : newRules){
			if(this.isBinaryRule(r) && !userFalseTuples.contains(r.getResult())){
				int cond = r.getCondIds().iterator().next();
				int result = r.getResult();
				Set<Integer> depSet = depMap.get(cond);
				if(depSet == null){
					depSet = new HashSet<Integer>();
					depMap.put(cond, depSet);
				}
				depSet.add(result);
				
				Set<Integer> revDepSet = revDepMap.get(result);
				if(revDepSet == null){
					revDepSet = new HashSet<Integer>();
					revDepMap.put(result, revDepSet);
				}
				revDepSet.add(cond);
				
				workList.add(cond);
				workList.add(result);
			}
		}
		
		while(!workList.isEmpty()){
			int n = workList.iterator().next();
			workList.remove(n);
			Set<Integer> oriDomSet = domMap.get(n);
			Set<Integer> newDomSet = new HashSet<Integer>();
			newDomSet.add(n);
			Set<Integer> revDepSet = revDepMap.get(n);
			Set<Integer> commPreDomSet = null;
			if(revDepSet != null){
				for(int rd : revDepSet){
					Set<Integer> pDom = domMap.get(rd);
					if(pDom == null){
						commPreDomSet = new HashSet<Integer>();
						break;
					}
					if(commPreDomSet == null)
						commPreDomSet = new HashSet<Integer>(pDom);
					else{
						commPreDomSet.retainAll(pDom);
					}
				}
			}
			if(commPreDomSet != null)
				newDomSet.addAll(commPreDomSet);
			domMap.put(n, newDomSet);
			if(!newDomSet.equals(oriDomSet)){
				Set<Integer> depSet = depMap.get(n);
				if(depSet != null)
					workList.addAll(depSet);
			}
		}
		
		// keep the top most dominator
		Set<Integer> selfDoms = new HashSet<Integer>();
		for(Map.Entry<Integer,Set<Integer>> entry : domMap.entrySet()){
			int k = entry.getKey();
			Set<Integer> v = entry.getValue();
			if(v.size() == 1){
				if(!v.contains(k))
					throw new RuntimeException("Something went wrong in dominator calculation.");
				selfDoms.add(k);
			}
		}
		
		for(Map.Entry<Integer, Set<Integer>> entry : domMap.entrySet()){
			Set<Integer> v = entry.getValue();
			v.retainAll(selfDoms);
			v.remove(entry.getKey());
		}
		
		Map<Integer,Integer> singleDomMap = new HashMap<Integer,Integer>();
		for(Map.Entry<Integer, Set<Integer>> entry : domMap.entrySet()){
			Set<Integer> v = entry.getValue();
			if(v.size() > 1)
				throw new RuntimeException("Something went wrong in dominator calculation.");
			if(!v.isEmpty()){
				singleDomMap.put(entry.getKey(), v.iterator().next());
			}
		}
		
		Set<InferenceRule> currentRules = newRules;
		newRules = new HashSet<InferenceRule>();
		for(InferenceRule r : currentRules){
			int result = r.getResult();
			if(singleDomMap.containsKey(result))
				continue;
			List<Integer> conds = r.getCondition();
			List<Integer> nConds = new ArrayList<Integer>();
			for(int c : conds){
				if(c < 0)
					nConds.add(c);
				if(singleDomMap.containsKey(c)){
					nConds.add(singleDomMap.get(c));
				}
				else
					nConds.add(c);
			}
			newRules.add(new InferenceRule(result, nConds, r.kind));
		}
		
		for(Map.Entry<Integer, Integer> entry : singleDomMap.entrySet()){
			int k = entry.getKey();
			int v = entry.getValue();
			if(reports.contains(k)){
				List<Integer> conds = new ArrayList<Integer>();
				conds.add(v);
				newRules.add(new InferenceRule(k,conds, InferenceRule.Kind.MAY));
			}
		}
		
		// Finally, construct the new analysis
		super.init(newRules, mln);
		this.fla = new FalseLabelAnalysis(newRules);
		this.externalFalseLabels = new LinkedHashSet<Integer>();
		this.externalTrueLabels = new LinkedHashSet<Integer>();
		System.out.println("# rules (after simplification): "+rules.size());
	}

	@Override
	public void labelAndPropagate(Set<Pair<Integer, Label>> labels) {
		for(Pair<Integer,Label> l : labels){
			if(l.right.equals(Label.TRUE))
				this.externalTrueLabels.add(l.left);
			if(l.right.equals(Label.FALSE))
				this.externalFalseLabels.add(l.left);
		}
		this.fla.setFalseLabel(externalFalseLabels);
		this.updateLabelMap();
	}	
	
	private void updateLabelMap(){
		super.labelMap.clear();
		for(int i : externalTrueLabels)
			super.labelMap.put(i, Label.TRUE);
		for(int i : externalFalseLabels)
			super.labelMap.put(i, Label.FALSE);
		for(int i : fla.getFalseTuples()){
			Label ori = this.labelMap.put(i, Label.FALSE);
			if(ori == Label.TRUE)
				throw new RuntimeException("Conflicting labels!");
		}
	}
	
}
