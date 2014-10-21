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

import sf.net.experimaestro.scheduler.Resource;

import java.util.HashMap;

/**
 * Scope variables when iterating over operators
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/12/13
 */
public class PlanScope {
    public HashMap<Resource, String> defaultLocks = null;

    @Override
    protected PlanScope clone() {
        PlanScope newScope = new PlanScope();
        newScope.defaultLocks = defaultLocks;
        return newScope;
    }

    public PlanScope clone(PlanScope scope) {
        PlanScope newScope = scope.clone();

        if (scope.defaultLocks != null) {
            newScope.defaultLocks = scope.defaultLocks;
        }


        return newScope;
    }
}
