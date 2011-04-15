package sf.net.experimaestro.utils;

import java.util.Map;
import java.util.Map.Entry;

public class Maps {

	/**
	 * Convert the values of a map
	 * 
	 * @param <Key>
	 * @param <Value1>
	 * @param <Value2>
	 * @param map1
	 * @param map2
	 * @param converter
	 */
	public static final <Key, Value1, Value2> Map<Key,Value2> convert(Map<Key, Value1> map1,
			Map<Key, Value2> map2, Converter<Value1, Value2> converter) {
		for (Entry<Key, Value1> entry : map1.entrySet())
			map2.put(entry.getKey(), converter.convert(entry.getValue()));
		return map2;
	}
}
