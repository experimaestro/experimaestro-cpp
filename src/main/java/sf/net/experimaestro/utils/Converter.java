package sf.net.experimaestro.utils;

public interface Converter<U,V> {
	/**
	 * Convert a value of type U into type V
	 * @param u The value of type U
	 * @return The value of type V
	 */
	public V convert(U u);
}
