package nichrome.mln.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Union-Find Data structure. By calling the makeUnionFind(S) a one element set
 * is made for every object s contained in S Objects are stored within Records
 * which are put in trees. A Set can be recognised by a toplevel record (with a
 * parent pointing to null)
 *
 * This not a general "Set handling" class, but made for being used with for
 * example Kruskal's Algortihm. See
 * http://en.wikipedia.org/wiki/Disjoint-set_data_structure for more info!
 * ------------------------------------- Implements makeUnionFind(S) in O(n),
 * Union in O(log n), or O(1) if you a record representing a set, and find(u)
 * called n times yieald an amortized running time of per calling of O(a(n)) ~=
 * O(1).
 *
 **/

public class UnionFind<E> {

	/* Record class. Defines a set, or a record within a set */
	@SuppressWarnings("hiding")
	public class Record<E> {
		private int size;
		private double weight;
		private E name;
		private Record<E> parent = null;
		private HashSet<Record<E>> kids = null;

		public Record(E name) {
			this.name = name;
			this.size = 1;
			this.weight = 1;
			if (UnionFind.this.trackKids) {
				this.kids = new HashSet<Record<E>>();
			}
		}

		public void setWeight(double wt) {
			this.weight = wt;
		}

		public void setParent(Record<E> parent) {
			if (this.parent == parent) {
				return;
			}
			if (UnionFind.this.trackKids) {
				Record<E> opa = this.parent;
				if (opa != null) {
					opa.kids.remove(this);
				}
				if (parent != null) {
					parent.kids.add(this);
				}
			}
			this.parent = parent;
		}

		public HashSet<E> getAllKids() {
			if (!UnionFind.this.trackKids) {
				return null;
			}
			HashSet<E> ret = new HashSet<E>();
			for (Record<E> k : this.kids) {
				ret.add(k.getName());
				ret.addAll(k.getAllKids());
			}
			return ret;
		}

		public boolean isRoot() {
			return this.parent == null;
		}

		public E getName() {
			return this.name;
		}

		private int getSize() {
			return this.size;
		}

		private double getWeight() {
			return this.weight;
		}

		private void absorb(Record<E> sub) {
			sub.setParent(this);
			this.size += sub.size;
			this.weight += sub.weight;
			UnionFind.this.nClusters--;
		}

		public Record<E> getParent() {
			return this.parent;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean equals(Object obj) {
			if (obj == null || this.getClass() != obj.getClass()) {
				return false;
			} else {
				Record<E> o = (Record<E>) obj;
				return this.name.equals(o.getName());
			}
		}
	}

	public HashSet<E> getAllNodesInCluster(E e) {
		E root = this.getRoot(e);
		HashSet<E> ret = this.map.get(root).getAllKids();
		ret.add(root);
		return ret;
	}

	private boolean trackKids = false;

	public UnionFind(boolean trackKids) {
		this.trackKids = trackKids;
	}

	public UnionFind() {
	}

	/**
	 * Separate a node into a singleton cluster
	 *
	 * @param e
	 */
	@SuppressWarnings("unchecked")
	public void splitNode(E e) {
		if (!this.trackKids) {
			UIMan
				.error("Cannot split node from a union find structure with trackKids = false.");
			return;
		}
		if (e == null) {
			return;
		}
		Record<E> node = this.map.get(e);
		Record<E> opa = node.parent;

		if (opa == null && node.kids.isEmpty()) {
			return;
		}

		// split from ancestors
		node.setParent(null);
		this.nClusters++;
		// split from kids
		if (!node.kids.isEmpty()) {
			if (opa != null) {
				// this node has a parent; attach all kids to the parent
				for (Record<E> kid : (HashSet<Record<E>>) node.kids.clone()) {
					kid.setParent(opa);
				}
			} else {
				// this node is a root; need to select a new root for the kids
				Record<E> nroot = null;
				for (Record<E> kid : node.kids) {
					nroot = kid;
					break;
				}
				nroot.setParent(null);
				// now nroot should be no longer a kid
				for (Record<E> kid : (HashSet<Record<E>>) node.kids.clone()) {
					kid.setParent(nroot);
				}
			}
		}
	}

	/* data member - ArrayList containing all the records */
	private ArrayList<Record<E>> records = new ArrayList<Record<E>>();
	private HashMap<E, Record<E>> map = new HashMap<E, Record<E>>();

	private int nClusters = 0;

	public int getNumClusters() {
		return this.nClusters;
	}

	public HashSet<E> getRoots() {
		HashSet<E> roots = new HashSet<E>();
		for (Record<E> rec : this.records) {
			if (rec.isRoot()) {
				roots.add(rec.getName());
			}
		}
		return roots;
	}

	/* Initalizes all sets, one for every element in list set */
	public void makeUnionFind(List<E> Set, HashMap<E, Double> wts) {
		for (E it : Set) {
			Record<E> rec = new Record<E>(it);
			if (wts.containsKey(it)) {
				rec.setWeight(wts.get(it));
			}
			this.records.add(rec);
			this.map.put(it, rec);
		}
		this.nClusters = this.map.size();
	}

	public void makeUnionFind(List<E> Set) {
		for (E it : Set) {
			Record<E> rec = new Record<E>(it);
			this.records.add(rec);
			this.map.put(it, rec);
		}
		this.nClusters = this.map.size();
	}

	public void addSingleton(E node, Double wts) {
		if (this.map.containsKey(node)) {
			return;
		}

		Record<E> rec = new Record<E>(node);
		rec.setWeight(wts);
		this.records.add(rec);
		this.map.put(node, rec);
	}

	public ArrayList<Record<E>> getRecords() {
		return this.records;
	}

	/* "Unionizes two sets */
	public E unionByValue(E xx, E yy) {
		Record<E> x = this.map.get(xx);
		Record<E> y = this.map.get(yy);
		Record<E> xroot = this.find(x);
		Record<E> yroot = this.find(y);

		if (xroot == yroot) {
			return xroot.name;
		}

		if ((Integer) xroot.name < (Integer) yroot.name) {
			xroot.absorb(yroot);
			return xroot.name;
		} else {
			yroot.absorb(xroot);
			return yroot.name;
		}
	}

	/* "Unionizes two sets */
	public E union(E xx, E yy) {
		Record<E> x = this.map.get(xx);
		Record<E> y = this.map.get(yy);
		Record<E> xroot = this.find(x);
		Record<E> yroot = this.find(y);

		if (xroot == yroot) {
			return xroot.name;
		}

		if (xroot.getSize() > yroot.getSize()) {
			xroot.absorb(yroot);
			return xroot.name;
		} else {
			yroot.absorb(xroot);
			return yroot.name;
		}
	}

	/* "Unionizes two sets */
	public E unionWithOrder(E xx, E yy) {
		Record<E> x = this.map.get(xx);
		Record<E> y = this.map.get(yy);
		Record<E> xroot = this.find(x);
		Record<E> yroot = this.find(y);

		if (xroot == yroot) {
			return xroot.name;
		}

		xroot.absorb(yroot);
		return xroot.name;
	}

	public int clusterSize(E x) {
		Record<E> rec = this.map.get(x);
		return this.find(rec).getSize();
	}

	public double clusterWeight(E x) {
		Record<E> rec = this.map.get(x);
		return this.find(rec).getWeight();
	}

	public HashMap<E, E> getPartitionMap() {
		HashMap<E, E> pmap = new HashMap<E, E>();
		for (Record<E> rec : this.records) {
			pmap.put(rec.getName(), this.find(rec).getName());
		}
		return pmap;
	}

	public E getRoot(E x) {
		return this.find(this.map.get(x)).getName();
	}

	/*
	 * Given a records returns the top-record that represents the set containing
	 * that record. Re-links the given record to the top-record (Path
	 * compression, the key to gain the amortized running time).
	 */
	public Record<E> find(Record<E> rec) {
		if (rec.getParent() == null) {
			return rec;
		} else {
			rec.setParent(this.find(rec.getParent()));
			return rec.getParent();
		}
	}

	/* Checks if to records are in the same set. */
	public boolean sameSet(Record<E> r1, Record<E> r2) {
		return this.find(r1).equals(this.find(r2));
	}

}
