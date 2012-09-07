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
import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.XMLUtils;

import java.util.Map;

/**
 * Handles alternatives
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class AlternativeValue extends Value {

	/**
	 * 
	 */
	private  AlternativeInput alternativeInput;

	/**
	 * The real task
	 */
	private Task task;

	/**
	 * The returned value
	 */
	private Document value = null;

	
	public AlternativeValue() {
	}
	
	@Override
	protected void init(Value _other) {
		AlternativeValue other = (AlternativeValue)_other;
		super.init(other);
		
		alternativeInput = other.alternativeInput;
		// Copy the task if it has been set
		if (task != null)
			task = task.copy();
	}
	
	/**
	 * Creates an alternative task object
	 * @param alternativeInput TODO
	 * 
	 * @param type The type of the alternative
	 */
	protected AlternativeValue(AlternativeInput alternativeInput, AlternativeType type) {
		super(alternativeInput);
		this.alternativeInput = alternativeInput;
	}

	@Override
	public void set(DotName id, Document value) {
		if (id.size() == 0) {
			final Map<QName, TaskFactory> factories = this.alternativeInput.alternativeType.factories;
			
			final Element element = value.getDocumentElement();
			String key = element.getAttributeNS(Manager.EXPERIMAESTRO_NS,
					"value");
			QName qname = XMLUtils.parseQName(key, element,
					Manager.PREDEFINED_PREFIXES);
			TaskFactory subFactory = factories.get(qname);
			if (subFactory == null)
				throw new ExperimaestroRuntimeException(
						"Could not find an alternative with name [%s]", key);
			AlternativeInput.LOGGER.info("Creating a task [%s]", subFactory.id);
			task = subFactory.create();
			AlternativeInput.LOGGER.info("Created the task for alternative [%s]", key);
		} else {
			task.setParameter(id, value);
		}
	}

	@Override
	public void process() {
		// If the task has not been set, try to use default value
		if (task == null && input.defaultValue != null)
			set(DotName.EMPTY, input.defaultValue);
		
		if (task == null)
			throw new ExperimaestroRuntimeException(
					"Alternative task has not been set");
		AlternativeInput.LOGGER.info("Running the alternative task [%s]",
				task.factory != null ? "n/a" : task.factory.id);
		value = task.run();
	}

	@Override
	public Document get() {
		return value;
	}



}