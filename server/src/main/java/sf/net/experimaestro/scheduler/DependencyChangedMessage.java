package sf.net.experimaestro.scheduler;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

/**
 * A message
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class DependencyChangedMessage extends Message {
    long fromId;
    DependencyStatus status;

    public DependencyChangedMessage(Dependency dependency, DependencyStatus from, DependencyStatus status) {
        super(Type.DEPENDENCY_CHANGED);
        this.fromId = dependency.getFrom().getId();
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Dependency changed [%d / %s]", fromId, status);
    }
}
