package nichrome.mln.learn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Pair;

import nichrome.mln.Atom;
import nichrome.mln.Clause;
import nichrome.mln.GClause;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.Predicate;
import nichrome.mln.Clause.ClauseInstance;
import nichrome.mln.db.RDB;
import nichrome.mln.infer.Engine;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.Config;
import nichrome.mln.util.FileMan;
import nichrome.mln.util.StringMan;
import nichrome.mln.util.Timer;
import nichrome.mln.util.UIMan;

public class Learner {
	HashMap<String, Pair<Double, Double>> trainingViolationMap;
	HashMap<String, Pair<Double, Double>> currentViolationMap;
	HashMap<String, Double> originalWeightsMap;
	HashMap<String, Double> currentWeightsMap;
	HashMap<String, Double> finalWeightsMap;
	Map<String, ClauseInstance> clauseInstanceIDMap;
	Map<String, Clause> clauseIDMap;

	public Learner() {
		trainingViolationMap = new HashMap<String, Pair<Double, Double>>();
		currentViolationMap = new HashMap<String, Pair<Double, Double>>();
		currentWeightsMap = new HashMap<String, Double>();
		finalWeightsMap = new HashMap<String, Double>();
	}

	public void learn(CommandOptions options) {
		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		UIMan.println(">>> Connecting to RDBMS at " + Config.db_url);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.setDB(db);
		String[] progFiles = options.fprog.split(",");
		mln.loadPrograms(progFiles);
		String[] eviFiles = options.fevid.split(",");
		mln.loadEvidences(eviFiles);
		String[] trainFiles = options.ftrain.split(",");
		mln.loadTrainData(trainFiles);
		mln.materializeTables();
		mln.prepareDB(db);

		Engine engine = new Engine(mln, db);
		clauseIDMap = mln.getClauseIDMap();
		clauseInstanceIDMap = mln.getClauseInstanceIDMap();
		trainingViolationMap = engine.countViolations();
		engine.clearAll();

		if (Config.gcLoadFile != null) {
			UIMan.println("Loading grounded clauses from file "
				+ Config.gcLoadFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.gcLoadFile);
				engine.loadGroundedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (Config.revLoadFile != null) {
			UIMan.println("Loading grounded reverted constrains from file "
				+ Config.revLoadFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.revLoadFile);
				engine.loadRevertedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (Config.revFdbkFile != null) {
			UIMan.println("Loading grounded feedback constrains from file "
				+ Config.revFdbkFile + ".");
			try {
				FileInputStream in = FileMan.getFileInputStream(Config.revFdbkFile);
				engine.loadRevertedConstraints(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		if(Config.fullyGround){
			UIMan.println("Starting fully ground");
			engine.fullyGround();
			UIMan.println("Done fully grounding");
		}
		originalWeightsMap = mln.getWeights();
		currentWeightsMap =
			(HashMap<String, Double>) originalWeightsMap.clone();
		fillInCurrentWeight();
		engine.updateWeights(currentWeightsMap);

		// OUTPUT THE INIT. WEIGHT AND ID/CLAUSE MAPPING
		Object[] keySet = currentWeightsMap.keySet().toArray();
		java.util.Arrays.sort(keySet);
		UIMan.println("#################INIT. WEIGHT#################");
		for (Object ss : keySet) {
			String s = (String) ss;
			Clause c = clauseIDMap.get(s);
			ClauseInstance ci = clauseInstanceIDMap.get(s);
			String desc = "";
			if (c != null) {
				desc += c.toString(-1);
			} else {
				desc += ci.parent.toString(ci.getId());
			}
			UIMan.println(s + "\t" + currentWeightsMap.get(s) + ":"
				+ trainingViolationMap.get(s).right + "/"
				+ this.trainingViolationMap.get(s).left + "\t" + desc);
		}

		UIMan.println(">>> Iteration Begins...");

		HashMap<String, Double> oldWeightsMap =
			(HashMap<String, Double>) currentWeightsMap.clone();
		for (int i = 0; i < options.nLIteration; i++) {
			currentViolationMap.clear();

			int total_solutions = 0;
			if (engine.run()) {
				
			/*	PrintWriter pw = FileMan.getPrintWriter(options.fout + "_" + i);
				for (int j : engine.getOptSolution()) {
					Atom at = mln.getAtom(j);
					pw.println(at.toGroundString(mln));
				}
				pw.flush();
				pw.close();
			*/	
				List<Set<Integer>> allSolutions = engine.getAllSolutions();
				for (int j = 0; j < Config.num_solver_solutions && j < allSolutions.size(); ++j) {
					++total_solutions;
					HashMap<String, Pair<Double, Double>> tempViolationMap = engine.countViolations(allSolutions.get(j));
					for (String clauseID : tempViolationMap.keySet()) {
						Pair<Double, Double> tmpVioCnt = tempViolationMap.get(clauseID);
						Pair<Double, Double> vioCnt = currentViolationMap.get(clauseID);
						if (vioCnt == null) {
							vioCnt = new Pair<Double, Double>(tmpVioCnt.left, tmpVioCnt.right);
						} else{
							vioCnt = new Pair<Double, Double>(vioCnt.left + tmpVioCnt.left, vioCnt.right + tmpVioCnt.right);
						}
						currentViolationMap.put(clauseID, vioCnt);
					}
				}
			} else {
				throw new RuntimeException(
					"Cannot train with UNSAT training data");
			}

			for (String clauseID : currentViolationMap.keySet()) {
				Pair<Double, Double> vioCnt = currentViolationMap.get(clauseID);
				vioCnt = new Pair<Double, Double>(vioCnt.left/total_solutions,
					vioCnt.right/total_solutions);
				currentViolationMap.put(clauseID, vioCnt);
			}
			
				if (updateWeight()) {
					break;
				}
				engine.updateWeights(currentWeightsMap);

				// CALC THE FINAL WEIGHT
				for (String s : currentWeightsMap.keySet()) {
					double fw = finalWeightsMap.get(s);
					fw = fw * (i + 1) + currentWeightsMap.get(s);
					fw /= (i + 2);
					finalWeightsMap.put(s, fw);
				}

				Object[] keySet1 = currentWeightsMap.keySet().toArray();
				java.util.Arrays.sort(keySet1);
				UIMan.println("#################ITERATION + " + i
					+ "#################");
				for (Object ss : keySet1) {
					String s = (String) ss;
					UIMan.println(s
						+ "\t"
						+ currentWeightsMap.get(s)
						+ "\t"
						+ (oldWeightsMap.get(s) < currentWeightsMap.get(s)
							? "larger\t" : "smaller\t")
						+ finalWeightsMap.get(s) + "\t"
						+ currentViolationMap.get(s).left + "->"
						+ trainingViolationMap.get(s).left);
				}

				oldWeightsMap =
					(HashMap<String, Double>) currentWeightsMap.clone();
				engine.clearAll();
		}
		Object[] keySet1 = currentWeightsMap.keySet().toArray();
		java.util.Arrays.sort(keySet1);
		UIMan.println("#################FINAL WEIGHT#################");
		for (Object ss : keySet1) {
			String s = (String) ss;
			UIMan.println(s + "\t" + currentWeightsMap.get(s) + "\t"
				+ finalWeightsMap.get(s) + "\t"
				+ currentViolationMap.get(s).left + "->"
				+ trainingViolationMap.get(s).left);
		}

		UIMan.println(">>> Writing answer to file: " + options.fout);
		dumpAnswers(mln, options.fout);

		if (Config.gcStoreFile != null) {
			UIMan.println("Storing grounded clauses to file "
				+ Config.gcStoreFile);
			try {
				FileOutputStream gcOut =
					new FileOutputStream(Config.gcStoreFile);
				engine.storeGroundedConstraints(gcOut);
				gcOut.flush();
				gcOut.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Updating current weights using Gradient Descent method.
	 */
	public boolean updateWeight() {
		double delta = 0;
		int n = 0;
		HashMap<String, Double> tempWeightsMap = new HashMap<String, Double>();
		boolean neg = false;
		for (String k : this.trainingViolationMap.keySet()) {
			n++;
			Double ev = this.currentViolationMap.get(k).left;
			Double cw = currentWeightsMap.get(k);
			assert (k != null);
			Double trainv = this.trainingViolationMap.get(k).left;
			if (trainv == null) {
				trainv = 0.0;
			}

			// if(cw>0 && cw*(cw + (0.01/(1)) * (ev - trainv))>0){
			if (cw > 0) {
				double newCW = cw + (0.001) * (ev - trainv);
				// delta += (newCW - cw) * (ev - trainv);
				delta += (ev - trainv) * (ev - trainv);
				if(newCW <0)
					neg = true;
				tempWeightsMap.put(k, newCW);
			}
			// if(cw<0 && cw*(cw - (0.01/(1)) * (ev - trainv))>0){
			if (cw < 0) {
				double newCW = cw - (0.001) * (ev - trainv);
				// delta += (newCW - cw) * (-(ev - trainv));
				delta += (ev - trainv) * (ev - trainv);
				if(newCW <0)
					neg = true;
				tempWeightsMap.put(k, newCW);
			}
			// this.currentWeight.put(k, Math.random());
		}
		if(neg){
			UIMan.println("Weights becoming negative, returning last weight solution");
			return true;
		}else{
			for(String k : tempWeightsMap.keySet()){
				currentWeightsMap.put(k, tempWeightsMap.get(k));
			}
		}
		UIMan.println("AVG. DELTA = " + delta / n);

		// Terminating Standard
		// if(Math.abs(delta) < 0.1)
		if (delta == 0) {
			return true;
		}

		return false;
	}

	/**
	 * Initialize weight according to the log odd of training data.
	 */
	public void fillInCurrentWeight() {
		double lowestNonVioWeight = Double.MAX_VALUE;
		double highestVioWeight = -Double.MAX_VALUE;
		boolean isNonVio = false;
		for (String s : trainingViolationMap.keySet()) {
			isNonVio = false;
			Clause c = clauseIDMap.get(s);
			ClauseInstance ci = clauseInstanceIDMap.get(s);
			if ((c != null && c.isHardClause())
				|| (ci != null && ci.isHardClause())) {
				finalWeightsMap.put(s, Config.hard_weight);
				currentWeightsMap.put(s, Config.hard_weight);
				continue;
			}

			double priorodds = 1;
			double tc, fc;
			// positive clause
			if (currentWeightsMap.get(s) > 0) {
				tc = this.trainingViolationMap.get(s).right;// satisfied count;
				fc = this.trainingViolationMap.get(s).left;// violated count
				if (tc == 0) {
					tc = 0.00001;
				}
				if (fc == 0) {
					fc = 0.00001;
					isNonVio = true;
				}
				if (tc == fc) {
					tc = fc + 0.001;
				}
				double cWeight =
					(Math.log(tc / fc)) - Math.log(priorodds);
				finalWeightsMap.put(s, cWeight);
				currentWeightsMap.put(s, cWeight);
				
				if (isNonVio) {
					if (cWeight < lowestNonVioWeight) {
						lowestNonVioWeight = cWeight;
					}
				} else {
					if (cWeight > highestVioWeight) {
						highestVioWeight = cWeight;
					}
				}

			} else {
				tc = this.trainingViolationMap.get(s).left;// violated count
				fc = this.trainingViolationMap.get(s).right;// satisfied count
				if (tc == 0) {
					tc = 0.00001;
					isNonVio = true;
				}
				if (fc == 0) {
					fc = 0.00001;
				}
				if (tc == fc) {
					tc = fc + 0.001;
				}
				double cWeight =
					(Math.log(tc / fc)) - Math.log(priorodds);
				finalWeightsMap.put(s, cWeight);
				currentWeightsMap.put(s, cWeight);

				 if (isNonVio) {
						if (-cWeight < lowestNonVioWeight) {
							lowestNonVioWeight = -cWeight;
						}
					} else {
						if (-cWeight > highestVioWeight) {
							highestVioWeight = -cWeight;
						}
					}
			}
		}
		if (lowestNonVioWeight < highestVioWeight) {
			int factor = (int) (Math.ceil(highestVioWeight/lowestNonVioWeight));
			for (String s : trainingViolationMap.keySet()) {
				isNonVio = false;
				Clause c = clauseIDMap.get(s);
				ClauseInstance ci = clauseInstanceIDMap.get(s);
				if ((c != null && c.isHardClause())
					|| (ci != null && ci.isHardClause())) {
					continue;
				}

				double tc, fc;
				// positive clause
				if (currentWeightsMap.get(s) > 0) {
					fc = this.trainingViolationMap.get(s).left;// violated count
					if (fc == 0) {
						isNonVio = true;
					}
					if (isNonVio) {	
						double cWeight = factor * currentWeightsMap.get(s);
						finalWeightsMap.put(s, cWeight);
						currentWeightsMap.put(s, cWeight);
					}
					
				} else {
					tc = this.trainingViolationMap.get(s).left;// violated count
					if (tc == 0) {
						isNonVio = true;
					}
					if (isNonVio) {
						double cWeight = factor * currentWeightsMap.get(s);
						finalWeightsMap.put(s, cWeight);
						currentWeightsMap.put(s, cWeight);
					}
				}
			}
		}
	}

	/**
	 * Dump the learning result to file {@link CommandOptions#fout}. The format
	 * of this file is consistent with inference part.
	 */
	public void dumpAnswers(MarkovLogicNetwork mln, String fout) {
		ArrayList<String> lines = new ArrayList<String>();
		DecimalFormat twoDForm = new DecimalFormat("#.####");

		HashSet<Predicate> allp = mln.getAllPred();
		for (Predicate p : allp) {
			String s = "";
			if (p.isClosedWorld()) {
				s += "*";
			}
			s += p.getName() + "(";
			for (int i = 0; i < p.arity(); i++) {
				s += p.getTypeAt(i).name();
				if (i != p.arity() - 1) {
					s += ",";
				}
			}
			s += ")";
			lines.add(s);
		}
		lines.add("\n");

		lines
			.add("//////////////AVERAGE WEIGHT OF ALL THE ITERATIONS//////////////");
		Object[] keySet = currentWeightsMap.keySet().toArray();
		java.util.Arrays.sort(keySet);
		for (Object ss : keySet) {
			String s = (String) ss;
			Clause c = clauseIDMap.get(s);
			ClauseInstance ci = clauseInstanceIDMap.get(s);

			if ((c != null && c.isHardClause())
				|| (ci != null && ci.isHardClause())) {
				if (c != null) {
					lines.add(c.toString(-1).replaceAll("^\\s+", "") + ". //"
						+ s);
				} else {
					lines.add(ci.parent.toString(ci.getId()).replaceAll(
						"^\\s+", "")
						+ ". //" + s);
				}
			} else {
				if (c != null) {
					lines.add(twoDForm.format(finalWeightsMap.get(s)) + " "
						+ c.toString(-1) + " //" + s);
				} else {
					lines.add(twoDForm.format(finalWeightsMap.get(s)) + " "
						+ ci.parent.toString(ci.getId()) + " //" + s);
				}
			}
		}

		lines.add("\n");

		lines.add("//////////////WEIGHT OF LAST ITERATION//////////////");
		keySet = currentWeightsMap.keySet().toArray();
		java.util.Arrays.sort(keySet);
		for (Object ss : keySet) {
			String s = (String) ss;
			Clause c = clauseIDMap.get(s);
			ClauseInstance ci = clauseInstanceIDMap.get(s);

			if ((c != null && c.isHardClause())
				|| (ci != null && ci.isHardClause())) {
				if (c != null) {
					lines.add(c.toString(-1).replaceAll("^\\s+", "") + ". //"
						+ s);
				} else {
					lines.add(ci.parent.toString(ci.getId()).replaceAll(
						"^\\s+", "")
						+ ". //" + s);
				}
			} else {
				if (c != null) {
					lines.add(twoDForm.format(finalWeightsMap.get(s)) + " "
						+ c.toString(-1) + " //" + s);
				} else {
					lines.add(twoDForm.format(finalWeightsMap.get(s)) + " "
						+ ci.parent.toString(ci.getId()) + " //" + s);
				}
			}

		}
		lines.add("\n\n");

		FileMan.writeToFile(fout, StringMan.join("\n", lines));
	}
}
