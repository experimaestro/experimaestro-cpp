package sf.net.experimaestro.plan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import bpiwowar.argparser.GenericHelper;

public class Var extends Node {
	String name;
	ArrayList<String> values = new ArrayList<String>();

	public String toString() {
		return String.format("%s=[%s]", name, Output.toString("],[", values));
	}

	@Override
	public Iterator<Map<String, String>> iterator() {
		return new AbstractIterator<Map<String, String>>() {
			Iterator<String> it = values.iterator();
			@Override
			protected boolean storeNext() {
				if (!it.hasNext())
					return false;
				
				value = GenericHelper.newTreeMap();
				value.put(name, it.next());
				
				return true;
			}
		};
	}
}