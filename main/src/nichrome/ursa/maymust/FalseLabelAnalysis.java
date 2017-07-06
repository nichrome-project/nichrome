package nichrome.ursa.maymust;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nichrome.datalog.utils.Pair;

public class FalseLabelAnalysis {
	private Set<InferenceRule> rules;
	private Map<Integer, LabelState> state;
	private Map<Integer, Set<InferenceRule>> headToRules;
	private Map<Integer, Set<InferenceRule>> bodyToRules;
	private Set<Integer> allTuples;
	private Set<Integer> externalFalseLabels;
	private Set<Integer> edbTuples;

	public FalseLabelAnalysis(Set<InferenceRule> rules) {
		this.rules = rules;
		this.init();
	}

	private void init() {
		this.state = new LinkedHashMap<Integer, LabelState>();
		this.headToRules = new LinkedHashMap<Integer, Set<InferenceRule>>();
		this.bodyToRules = new LinkedHashMap<Integer, Set<InferenceRule>>();
		this.allTuples = new HashSet<Integer>();
		this.externalFalseLabels = new HashSet<Integer>();
		for (InferenceRule r : rules) {
			for (int b : r.getCondition()) {
				if (b <= 0)
					continue; // simply ignore negative conds
				Set<InferenceRule> brs = bodyToRules.get(b);
				if (brs == null) {
					brs = new HashSet<InferenceRule>();
					bodyToRules.put(b, brs);
				}
				brs.add(r);
				this.allTuples.add(b);
			}
			int h = r.getResult();
			this.allTuples.add(h);
			Set<InferenceRule> hrs = headToRules.get(h);
			if (hrs == null) {
				hrs = new HashSet<InferenceRule>();
				headToRules.put(h, hrs);
			}
			hrs.add(r);
		}

		this.edbTuples = new HashSet<Integer>();

		for (int b : this.bodyToRules.keySet())
			if (!this.headToRules.containsKey(b))
				this.edbTuples.add(b);

		for (int t : allTuples) {
			this.state.put(t, LabelState.BOTTOM);
		}


		for (int t : this.edbTuples) {
			this.state.put(t, LabelState.UNKNOWN);
		}

		this.propagate(new ArrayList<Pair<Integer, LabelState>>());
		long t3 = System.currentTimeMillis();
	}

	private void propagate(List<Pair<Integer, LabelState>> changedLabels) {
		this.updateLabels(changedLabels);

		// rerun the analysis to a lfp
		Set<InferenceRule> workList = new LinkedHashSet<InferenceRule>();
		workList.addAll(rules);
		while (!workList.isEmpty()) {
			InferenceRule cr = workList.iterator().next();
			workList.remove(cr);
			boolean isAllUnknown = true;
			boolean ifUpdate = false;
			for (int b : cr.getCondition()) {
				if (b <= 0)
					continue;
				LabelState bl = this.state.get(b);
				isAllUnknown &= bl.equals(LabelState.UNKNOWN);
				if (bl.equals(LabelState.FALSE)) {
					ifUpdate |= this.setLabel(cr.getResult(), LabelState.FALSE);
					break;
				}
			}
			if (isAllUnknown){
//				System.out.println("Unknown produced for "+cr);
				ifUpdate |= this.setLabel(cr.getResult(), LabelState.UNKNOWN);
			}
			;
			if (ifUpdate) {
				Set<InferenceRule> brs = this.bodyToRules.get(cr.getResult());
				if (brs != null)
					workList.addAll(brs);
			}
		}
	}

	private LabelState join(LabelState l1, LabelState l2) {
		if (l1.compareTo(l2) > 0)
			return l1;
		return l2;
	}

	private boolean setLabel(int i, LabelState l) {
		if(this.externalFalseLabels.contains(i))
			return false;
		LabelState ol = this.state.get(i);
		LabelState nl = join(ol, l);
		this.state.put(i, nl);
//		if (!ol.equals(nl))
//			System.out.println("Setting " + i + " from " + ol + " to " + nl);
		return !ol.equals(nl);
	}

	private boolean resetLabelAndCheckIfDowngrade(int i) {
		if (this.externalFalseLabels.contains(i))
			return false;
		LabelState l = LabelState.BOTTOM;
		LabelState ori = this.state.get(i);
		this.state.put(i, l);
		return ori.compareTo(l) > 0; // downgrading state
	}

	private void updateLabels(List<Pair<Integer, LabelState>> changedLabels) {
		// if go down in the lattice, we need to reset the labels of the tuples
		// that depend on the
		// changed tuples
		List<Integer> downTuples = new ArrayList<Integer>();
		for (Pair<Integer, LabelState> p : changedLabels) {
			LabelState oriL = this.state.get(p.val0);
			if(oriL == null) //not on the derivation graph
				continue;
			if (p.val1 == LabelState.UNKNOWN) {
				throw new RuntimeException("Do not expect label to be set to UNKOWN!");
			}
			if (p.val1 == LabelState.FALSE) {
				this.externalFalseLabels.add(p.val0);
			}
			if (p.val1 == LabelState.BOTTOM) {
				this.externalFalseLabels.remove(p.val0);
			}
			this.state.put(p.val0, p.val1);
			if (oriL.compareTo(p.val1) > 0)
				downTuples.add(p.val0);
		}
		while (!downTuples.isEmpty()) {
			int i = downTuples.remove(downTuples.size() - 1);
			Set<InferenceRule> brs = this.bodyToRules.get(i);
			if (brs != null)
				for (InferenceRule r : brs) {
					if (this.resetLabelAndCheckIfDowngrade(r.getResult()))
						downTuples.add(r.getResult());
				}
		}
	}

	public void setFalseLabel(Set<Integer> ts) {
		List<Pair<Integer, LabelState>> fList = new ArrayList<Pair<Integer, LabelState>>();
		for (int t : ts)
			fList.add(new Pair<Integer, LabelState>(t, LabelState.FALSE));
		this.propagate(fList);
	}

	public Set<Integer> getFalseTuples() {
		Set<Integer> ret = new LinkedHashSet<Integer>();
		for (Map.Entry<Integer, LabelState> e : state.entrySet()) {
			if (e.getValue().equals(LabelState.FALSE))
				ret.add(e.getKey());
		}
		return ret;
	}
}

enum LabelState {
	BOTTOM, FALSE, UNKNOWN
}
