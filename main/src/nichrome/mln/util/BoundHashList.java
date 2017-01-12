package nichrome.mln.util;

import java.util.HashSet;
import java.util.LinkedList;

public class BoundHashList<T> {
	LinkedList<T> list = new LinkedList<T>();
	HashSet<T> set = new HashSet<T>();

	private int bound = Integer.MAX_VALUE;

	public boolean contains(T e) {
		return this.set.contains(e);
	}

	public BoundHashList(int maxSize) {
		this.bound = maxSize;
	}

	public boolean add(T e) {
		if (this.set.contains(e)) {
			return false;
		}
		if (this.list.size() >= this.bound) {
			this.set.remove(this.list.removeFirst());
		}
		this.list.addLast(e);
		this.set.add(e);
		return true;
	}
}
