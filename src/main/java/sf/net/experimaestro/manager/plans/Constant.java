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

package sf.net.experimaestro.manager.plans;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A constant in a plan just generate nodes
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class Constant extends Operator {
    List<Document> nodes = new ArrayList<>();

    public Constant(Document... documents) {
        this(Arrays.asList(documents));
    }

    public Constant(Iterable<Document> documents) {
        for (Document document : documents) {
            if (document instanceof DocumentImpl)
                ((DocumentImpl) document).setReadOnly(true,true);
            nodes.add(document);
        }
    }

    @Override
    public List<Operator> getParents() {
        return ImmutableList.of();
    }

    @Override
    protected Iterator<ReturnValue> _iterator(RunOptions runOptions) {
        return Iterators.transform(nodes.iterator(), new Function<Document, ReturnValue>() {
            @Override
            public ReturnValue apply(Document input) {
                return new ReturnValue(null, input);
            }
        });
    }


    @Override
    public void addParent(Operator parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        return new Constant(nodes);
    }

    @Override
    protected String getName() {
        return String.format("XML (%d)", nodes.size());
    }

    public void add(Constant source) {
        nodes.addAll(source.nodes);
    }

    public void add(Document document) {
        nodes.add(document);
    }
}
