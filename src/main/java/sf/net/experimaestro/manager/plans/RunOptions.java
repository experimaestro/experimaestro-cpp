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

import java.util.HashMap;
import java.util.Map;

/**
 * Options when running a plan
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 9/3/13
 */
final public class RunOptions {

    /** Static context */
    final static public class Context {
        /**
         * Whether the jobs are submitted to the scheduler or not
         */
        private boolean simulate;

        /**
         * Counts the number of items output by an operator; null if not used
         */
        private Map<Operator, MutableInt> counts;

        public Context(boolean simulate) {
            this.simulate = simulate;
        }
    }

    /**
     * The static context
     */
    Context context;

    /**
     * The dynamic context
     */
    PlanScope scope;


    private RunOptions(Context context) {
        this.context = context;
    }

    public RunOptions(boolean simulate) {
        context = new Context(simulate);
    }

    public RunOptions counts(boolean flag) {
        if (flag) context.counts = new HashMap<>();
        else context.counts = null;
        return this;
    }

    public Map<Operator, MutableInt> counts() {
        return context.counts;
    }

    public boolean simulate() {
        return context.simulate;
    }

    public RunOptions add(PlanScope scope) {
        RunOptions options = new RunOptions(context);
        options.scope = scope.clone(scope);
        return options;
    }

    public TaskContext getTaskContext() {
        return new TaskContext()
                .simulate(simulate())
                .defaultLocks(scope.defaultLocks);
    }

}
