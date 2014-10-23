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

import org.apache.commons.lang.mutable.MutableInt;
import sf.net.experimaestro.manager.TaskContext;
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.utils.CachedIterable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Context when running a plan
 * <p/>
 * This class hides away what is part of the static context and
 * what if part of the dynamic one
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 9/3/13
 */
final public class PlanContext {

    /**
     * The static context
     */
    StaticContext staticContext;

    /**
     * The dynamic context
     */
    DynamicContext dynamicContext;


    private PlanContext(StaticContext staticContext) {
        this.staticContext = staticContext;
    }


    public PlanContext(TaskContext taskContext) {
        staticContext = new StaticContext(taskContext);
    }

    public PlanContext counts(boolean flag) {
        if (flag) staticContext.counts = new HashMap<>();
        else staticContext.counts = null;
        return this;
    }

    public Map<Operator, MutableInt> counts() {
        return staticContext.counts;
    }

    public boolean simulate() {
        return staticContext.taskContext.simulate();
    }

    public PlanContext add(DynamicContext dynamicContext) {
        PlanContext options = new PlanContext(staticContext);
        options.dynamicContext = dynamicContext.clone(dynamicContext);
        return options;
    }

    public TaskContext getTaskContext() {
        return staticContext.taskContext;
    }

    public void setCachedIterable(Object key, CachedIterable<Value> cachedIterable) {
        staticContext.cachedIterables.put(key, cachedIterable);
    }

    public CachedIterable<Value> getCachedIterable(Object key) {
        return staticContext.cachedIterables.get(key);
    }

    public void setTaskOperatorMap(IdentityHashMap<TaskOperator, TaskReference> taskOperatorMap) {
        this.staticContext.taskOperatorMap = taskOperatorMap;
    }

    public void setTaskOperator(TaskOperator taskOperator) {
        staticContext.taskContext.setTask(taskOperator == null ? null : staticContext.taskOperatorMap.get(taskOperator));
    }

    /**
     * Static context
     */
    final static public class StaticContext {
        /**
         * The task context
         */
        final private TaskContext taskContext;
        /**
         * Counts the number of items output by an operator; null if not used
         */
        private Map<Operator, MutableInt> counts;
        /**
         * Cached iterators
         */
        private IdentityHashMap<Object, CachedIterable<Value>> cachedIterables = new IdentityHashMap<>();

        /**
         * The task operator map
         */
        private IdentityHashMap<TaskOperator, TaskReference> taskOperatorMap = new IdentityHashMap<>();

        public StaticContext(TaskContext taskContext) {
            this.taskContext = taskContext;
        }
    }

}
