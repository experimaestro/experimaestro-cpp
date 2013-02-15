/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.utils.ArrayNodeList;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.*;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 9/2/13
 */
abstract public class Mappings implements Iterable<Mapping> {

    /**
     * Initialisation.
     * <p/>
     * It can return another Mappings object if the initialization changes
     * its state
     */
    abstract public Mappings init(PlanNode planNode) throws XPathExpressionException;

    /**
     * Add all the plans contained in the mapping
     * @param subplans
     */
    public abstract void addPlans(HashSet<Plan> subplans);




    /**
     * A reference for a mapping
     */
    static public class Reference {
        Plan plan;
        PlanNode node;
        XPathExpression xpath;

        public Reference(Plan plan, String xpath) throws XPathExpressionException {
            this(plan, xpath == null ? null : XMLUtils.parseXPath(xpath));
        }

        public Reference(Plan plan, XPathExpression xpath) {
            this.plan = plan;
            this.xpath = xpath;
        }
    }

    /**
     * Function to transform inputs
     */
    public interface Function {
        Node f(Node[] parameters);
    }

    /**
     * Identity function
     */
    static public class IdentityFunction implements Function {
        public final static IdentityFunction INSTANCE = new IdentityFunction();

        private IdentityFunction() {
        }

        @Override
        public Node f(Node[] parameters) {
            if (parameters.length != 1)
                throw new AssertionError();
            return parameters[0];
        }
    }


    /**
     * Connections between plans
     */
    static public class Connection {
        Function function;
        List<Reference> parameters;

        public Connection(Function function, Reference... parameters) {
            this(function, Arrays.asList(parameters));
        }

        public Connection(Function function, List<Reference> parameters) {
            this.function = function;
            this.parameters = parameters;
        }

        Connection init(PlanNode planNode) throws XPathExpressionException {
            final ArrayList<Reference> references = new ArrayList<>();
            for (Reference reference : this.parameters) {
                boolean ok = false;
                for (PlanNode parent : planNode.getParents())
                    if (parent.getPlan() == reference.plan) {
                        final Reference e = new Reference(reference.plan, reference.xpath);
                        references.add(e);
                        e.node = parent;
                        ok = true;
                        break;
                    }

                if (!ok)
                    throw new AssertionError("Could not find the connection");
            }
            return new Connection(function, references);
        }
    }

    /**
     * Maps a name to several values
     */
    static public class Simple extends Mappings {
        DotName id;
        ArrayList<Object> values = new ArrayList<>();

        /**
         * @param id
         * @param values can be a {@linkplain org.w3c.dom.Node}, a {@linkplain String}
         *               or a connection
         */
        public Simple(DotName id, Object... values) {
            this.id = id;
            this.values.addAll(Arrays.asList(values));
        }

        public void add(Document value) {
            values.add(value);
        }

        @Override
        public Mappings init(PlanNode planNode) throws XPathExpressionException {
            Object[] newValues = new Object[values.size()];
            for (int i = 0; i < values.size(); i++)
                if (values.get(i) instanceof Connection)
                    newValues[i] = ((Connection) values.get(i)).init(planNode);
                else
                    newValues[i] = values.get(i);

            return new Simple(id, newValues);
        }

        @Override
        public void addPlans(HashSet<Plan> subplans) {
            for (Object value : values)
                if (value instanceof Connection)
                    for (Reference ref : ((Connection) value).parameters)
                        subplans.add(ref.plan);
        }

        @Override
        public Iterator<Mapping> iterator() {
            return new AbstractIterator<Mapping>() {
                Iterator<Object> iterator = values.iterator();


                protected Mapping computeNext() {
                    if (!iterator.hasNext())
                        return endOfData();
                    final Object next = iterator.next();

                    return new Mapping() {
                        @Override
                        public void set(Task task) throws NoSuchParameter, XPathExpressionException {
                            if (next instanceof Node)
                                task.setParameter(id, (Document) next);
                            else if (next instanceof Connection) {
                                Connection connection = (Connection) next;
                                final Node[] nodes = new Node[connection.parameters.size()];
                                for (int i = 0; i < nodes.length; i++) {
                                    final Reference reference = connection.parameters.get(i);
                                    final Node node = reference.node.value;


                                    final NodeList list = reference.xpath == null ? new ArrayNodeList(node)
                                            : (NodeList) reference.xpath.evaluate(node, XPathConstants.NODESET);
                                    if (list.getLength() == 0)
                                        throw new ExperimaestroRuntimeException("XPath [%s] did not return any result", reference.xpath);

                                    nodes[i] = XMLUtils.toDocumentFragment(list);


                                }
                                task.setParameter(id, connection.function.f(nodes));
                            } else task.setParameter(id, (String) next);
                        }

                    };
                }
            };
        }
    }



    /**
     * An abstract list of mappings
     */
    public abstract static class _List extends Mappings {
        ArrayList<Mappings> list;

        public _List(java.util.List<Mappings> list) {
            this.list = new ArrayList(list);
        }

        @Override
        public Mappings init(PlanNode planNode) throws XPathExpressionException {
            ArrayList<Mappings> newList = new ArrayList<>();
            for (Mappings mappings : list)
                newList.add(mappings.init(planNode));
            return clone(newList);
        }

        protected abstract Mappings clone(ArrayList<Mappings> list);

        public void add(Mappings mappings) {
            list.add(mappings);
        }

        @Override
        public void addPlans(HashSet<Plan> subplans) {
            for (Mappings mappings : list)
                mappings.addPlans(subplans);
        }
    }

    /**
     * Product of mappings
     */
    static public class Product extends _List {

        public Product(Mappings... list) {
            this(Arrays.asList(list));
        }

        public Product(java.util.List<Mappings> list) {
            super(list);
        }

        @Override
        protected Mappings clone(ArrayList<Mappings> list) {
            return new Product(list);
        }


        @Override
        public Iterator<Mapping> iterator() {
            final Iterator<Mapping[]> iterator = new CartesianProduct(Mapping.class, list.toArray(new Iterable[list.size()])).iterator();

            return new AbstractIterator<Mapping>() {
                @Override
                protected Mapping computeNext() {
                    if (!iterator.hasNext())
                        return endOfData();
                    final Mapping[] mappings = iterator.next();
                    return new Mapping() {
                        @Override
                        public void set(Task task) throws NoSuchParameter, XPathExpressionException {
                            for (Mapping mapping : mappings)
                                mapping.set(task);

                        }
                    };
                }
            };
        }

    }

    /**
     * A list of alternative mappings
     */
    static public class Alternative extends _List {
        public Alternative(Mappings... list) {
            this(Arrays.asList(list));
        }

        public Alternative(java.util.List<Mappings> list) {
            super(list);
        }

        @Override
        protected Mappings clone(ArrayList<Mappings> list) {
            return new Alternative(list);
        }

        @Override
        public Iterator<Mapping> iterator() {
            Iterator<Mapping> iterators[] = new Iterator[list.size()];
            for(int i = 0; i < iterators.length; i++)
                iterators[i] = list.get(i).iterator();

            return Iterators.concat(iterators);
        }
    }

}
