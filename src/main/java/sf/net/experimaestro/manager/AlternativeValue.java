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
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Map;

/**
 * Handles alternatives
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class AlternativeValue extends Value {
    static final private Logger LOGGER = Logger.getLogger();

    /**
     *
     */
    private AlternativeInput alternativeInput;

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
        AlternativeValue other = (AlternativeValue) _other;
        super.init(other);

        alternativeInput = other.alternativeInput;
        // Copy the task if it has been set
        if (task != null)
            task = task.copy();
    }

    /**
     * Creates an alternative task object
     *
     * @param input The input
     */
    protected AlternativeValue(AlternativeInput input) {
        super(input);
        this.alternativeInput = input;
    }

    @Override
    public Value getValue(DotName id) throws NoSuchParameter {
        if (id.size() == 0)
            return this;

        return task.getValue(id);
    }

    @Override
    public void set(Document value) {
        // Check if we have an XML with the valid type
        final Element element = XMLUtils.getRootElement(value);
        if (alternativeInput.getType().qname().sameQName(element)) {
            // Nothing to do
            LOGGER.info("Alternative input already generated [%s]", alternativeInput.getType());
            this.value = value;
        } else {
            final Map<QName, TaskFactory> factories = ((AlternativeType) this.alternativeInput.type).factories;
            String key = Manager.unwrapToString(element);
            QName qname = QName.parse(key, element,
                    Manager.PREDEFINED_PREFIXES);
            TaskFactory subFactory = factories.get(qname);
            if (subFactory == null)
                throw new ExperimaestroRuntimeException(
                        "Could not find an alternative with name [%s]", key);
            LOGGER.info("Creating a task [%s]", subFactory.id);
            task = subFactory.create();
            AlternativeInput.LOGGER.info("Created the task for alternative [%s]", key);
        }
    }

    @Override
    public void process(boolean simulate) throws NoSuchParameter, ValueMismatchException {
        // If the value has not been set
        if (value == null) {
            // If the task has not been set, try to use default value
            if (task == null && input.defaultValue != null)
                set(input.defaultValue);

            if (task == null)
                throw new ExperimaestroRuntimeException(
                        "Alternative task has not been set");
            LOGGER.info("Running the alternative task [%s]",
                    task.factory != null ? "n/a" : task.factory.id);
            value = task.run(simulate);
        }
    }

    @Override
    public Document get() {
        return value;
    }


}