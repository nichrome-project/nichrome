package nichrome.mln;

public class CardinalityConstr {
	public enum Kind{AT_MOST, AT_LEAST};
	private Kind kind;
	private int ats[];
	private int k;
	
	/**
	 * At <kind> <k> atoms in <ats> are set to true
	 * @param kind
	 * @param ats
	 * @param k
	 */
	public CardinalityConstr(Kind kind, int[] ats, int k) {
		super();
		this.kind = kind;
		this.ats = ats;
		this.k = k;
	}

	public Kind getKind() {
		return kind;
	}

	public int[] getAts() {
		return ats;
	}

	public int getK() {
		return k;
	}
	
}
