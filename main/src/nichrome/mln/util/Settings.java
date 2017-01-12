package nichrome.mln.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Settings {
	private HashMap<String, Object> map = new HashMap<String, Object>();

	public Settings() {

	}

	@Override
	public String toString() {
		ArrayList<String> lines = new ArrayList<String>();
		for (String k : this.map.keySet()) {
			lines.add("    " + k + ": " + this.map.get(k).toString());
		}
		return StringMan.join("\n", lines);
	}

	public Settings(Map<String, Object> map) {
		this.map.putAll(map);
	}

	public void put(String k, Object v) {
		this.map.put(k, v);
	}

	public boolean hasKey(String k) {
		return this.map.containsKey(k);
	}

	public Object get(String k) {
		return this.map.get(k);
	}

	public Integer getInt(String k) {
		return (Integer) (this.map.get(k));
	}

	public Double getDouble(String k) {
		return (Double) (this.map.get(k));
	}

	public String getString(String k) {
		return (String) (this.map.get(k));
	}

	public Boolean getBool(String k) {
		return (Boolean) (this.map.get(k));
	}
}
