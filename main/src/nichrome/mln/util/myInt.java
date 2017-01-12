package nichrome.mln.util;

public class myInt {
	public int value = -1;

	public void addOne() {
		this.value++;
	}

	public void subOne() {
		this.value--;
	}

	public myInt(int _value) {
		this.value = _value;
	}

	@Override
	public String toString() {
		return "" + this.value;
	}

}
