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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.*;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/1/13
 */
@Entity
public class ResourceData {
    /**
     *
     */
    public static final String RESOURCE_ID_NAME = "resourceID";
    /**
     * Secondary key for "keys"
     */
    public static final String GROUP_KEY_NAME = "group";


    /**
     * The resource URI
     */
    @PrimaryKey
    ResourceLocator locator;

    /**
     * The rersouce ID
     */
    @SecondaryKey(name = RESOURCE_ID_NAME, relate = Relationship.ONE_TO_ONE, onRelatedEntityDelete = DeleteAction.CASCADE)
    Long resourceId;

    /**
     * Group this resource belongs to. Note that names are separated by one zero byte for sorting reasons
     */
    @SecondaryKey(name = GROUP_KEY_NAME, relate = Relationship.MANY_TO_ONE)
    GroupId groupId = new GroupId();


    protected ResourceData() {

    }

    public ResourceData(ResourceLocator locator) {
        this.locator = locator;
    }

    /**
     * Sets the group
     *
     * @param groupId
     */
    public void setGroupId(String groupId) {
        this.groupId = new GroupId(groupId);
    }


    public String getGroupId() {
        return groupId.getName();
    }


    public ResourceLocator getLocator() {
        return locator;
    }

    public ResourceData init(Scheduler scheduler) {
        locator.init(scheduler);
        return this;
    }


    public void setResourceID(long resourceID) {
        this.resourceId = resourceID;
    }
}
