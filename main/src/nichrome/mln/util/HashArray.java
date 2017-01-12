package nichrome.mln.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HashArray<T> {
	ArrayList<T> list = new ArrayList<T>();
	HashMap<T, Integer> indices = new HashMap<T, Integer>();
	Random rand = new Random();

	public int size = 0;

	public ArrayList<T> getList() {
		return this.list;
	}

	public T getRandomElement() {
		return this.list.get(this.rand.nextInt(this.list.size()));
	}

	public boolean contains(T e) {
		return this.indices.containsKey(e);
	}

	public void clear() {
		this.list.clear();
		this.indices.clear();
		this.size = 0;
	}

	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	public void add(T e) {
		if (this.indices.containsKey(e)) {
			return;
		}
		this.list.add(e);
		this.indices.put(e, this.list.size() - 1);
		this.size++;
	}

	public void removeIdx(int i) {
		int ss = this.list.size();
		if (i < 0 || i >= ss) {
			return;
		}
		this.indices.remove(this.list.get(i));
		if (i == ss - 1) {
			this.list.remove(i);
		} else {
			T last = this.list.get(ss - 1);
			this.indices.put(last, i);
			this.list.set(i, last);
			this.list.remove(ss - 1);
		}
		this.size--;
	}

	public void removeObj(T e) {
		if (!this.indices.containsKey(e)) {
			return;
		}
		int i = this.indices.get(e);
		this.removeIdx(i);
	}
}
