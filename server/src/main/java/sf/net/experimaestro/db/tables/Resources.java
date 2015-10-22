package sf.net.experimaestro.db.tables;

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

import sf.net.experimaestro.db.*;
import sf.net.experimaestro.scheduler.Scheduler;

import java.sql.SQLException;

/**
 * Resources
 */
public class Resources extends Table {
    final static private Resources T = new Resources();

    @Identity
    Field<Long> id;

    @NotNull
    @MaxSize(255)
    Field<String> path;

    @NotNull
    Field<Integer> status;

    @NotNull
    @IntegerType(bytes = 2)
    Field<Integer> oldStatus;

    @NotNull
    @IntegerType(bytes = 2)
    Field<Integer> type;

    @NotNull
    @IntegerType(bytes = 2)
    @Default("0")
    Field<Integer> priority;

    @NotNull
    @IntegerType(bytes = 4)
    Field<Long> connector;

    @NotNull
    Field<byte[]> data;

    @Override
    protected void init() {
        super.init();

    }

    static void test() throws SQLException {
        final PlaceHolder path = new PlaceHolder();
        final Query select = T.ref().select(T.id, T.type, T.path, T.status)
                .where(Db.eq(T.path, path));

        select.execute(Scheduler.get().getConnection());
    }

}
