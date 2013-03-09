/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.manager;

import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * 
 * A simple XML value
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XMLValue extends Value {
	final static private Logger LOGGER = Logger.getLogger();

	private Document value;

	public XMLValue(Input input) {
		super(input);
	}

	public XMLValue() {
	}

	@Override
	public void process(boolean simulate) {
		// If there is no value, takes the default
		if (value == null && input.defaultValue != null) {
			LOGGER.debug("Setting default value [%s]",
					XMLUtils.toStringObject(input.defaultValue));
			value = (Document) input.defaultValue.cloneNode(true);
		}
	}

	@Override
	public Value getValue(DotName id) {
		if (id.size() != 0)
			throw new ExperimaestroRuntimeException(
					"Cannot handle qualified names [%s]");
		LOGGER.debug("Value set to [%s]", XMLUtils.toString(value));
        return this;
	}

    @Override
    public void set(Document value) {
        LOGGER.debug("Value set to [%s]", XMLUtils.toString(value));
        this.value = value;
    }

	@Override
	public Document get() {
		return value;
	}

	@Override
	protected void init(Value _other) {
		XMLValue other = (XMLValue) _other;
		super.init(other);
		if (other.value != null)
			value = (Document) other.value.cloneNode(true);
	}

    @Override
    public boolean isSet() {
        return value != null;
    }
}
