package sf.net.experimaestro.db;

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

import sf.net.experimaestro.scheduler.Scheduler;

import java.sql.Blob;
import java.sql.SQLException;

/**
 * Resources
 */
public class Resources extends Table {
    public static Resources instance = new Resources();

    @Identity
    public
    Field<Long> id;

    @NotNull
    @MaxSize(4096)
    public
    Field<String> path;

    @NotNull
    public
    Field<Long> status;

    @NotNull
    public
    Field<Long> type;

    @NotNull
    @Default("0")
    Field<Integer> priority;

    @NotNull
    @References(Connectors.class)
    Field<Long> connector;

    @NotNull
    Field<Blob> data;

    static void test() throws SQLException {
        final sf.net.experimaestro.db.Resources resources = Table.create(sf.net.experimaestro.db.Resources.class);

        final PlaceHolder path = new PlaceHolder();
        final Query select = resources.ref().select(resources.id, resources.type, resources.path, resources.status)
                .where(Db.eq(resources.path, path));

        select.execute(Scheduler.get().getConnection());
    }


}
