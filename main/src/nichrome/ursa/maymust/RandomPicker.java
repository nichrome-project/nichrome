package nichrome.ursa.maymust;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import nichrome.mln.Atom;
import nichrome.mln.MarkovLogicNetwork;
import nichrome.mln.parser.CommandOptions;
import nichrome.mln.util.FileMan;

public class RandomPicker implements QuestionPicker {

	protected MarkovLogicNetwork mln;
	protected Set<Integer> searchSpace;
	protected CommandOptions options;
	
	public void init(MarkovLogicNetwork mln,CommandOptions options){
		this.mln = mln;
		this.options = options;
		searchSpace = new HashSet<Integer>();
		this.loadSearchSpaceTuples(options.ursaSearchSpace);
	}
	
	private void loadSearchSpaceTuples(String file){
		try{
			FileInputStream in = FileMan.getFileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.startsWith("//"))
					continue;
				line = line.trim();
				if(line.equals(""))
					continue;
				Atom at = this.mln.parseAtom(line);
				int atId = this.mln.getAtomID(at);
				searchSpace.add(atId);
			}
			br.close();
		}catch(IOException e){
			e.printStackTrace();
			throw new RuntimeException("Loading Search space file failed with "+e.toString());
		}
	}
	@Override
	public List<Integer> pick(AnalysisWithAnnotations a, int lr) {
		int numFeedback = this.options.numFeedback;
		if(numFeedback == -1){
			throw new RuntimeException("Batch mode not supported");
		}
		List<Integer> unlabeled = new ArrayList<Integer>();
		for(Integer atId : searchSpace){
			if(a.isAtLabeled(atId))
				continue;
			unlabeled.add(atId);
		}
		List<Integer> ret = new ArrayList<Integer>();
		if(unlabeled.size()==0)
			return ret;
		int i = 0;
		Random r = new Random();
		for(i=0;i<numFeedback;i++){
			int chosen = r.nextInt(unlabeled.size());
			ret.add(unlabeled.get(chosen));
			unlabeled.remove(chosen);
			if(unlabeled.size()==0)
				break;
		}
		return ret;
	}

	@Override
	public void simplify(AnalysisWithAnnotations a) {
		// TODO Auto-generated method stub
		
	}

}
