/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2016 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.documentation;

import java.io.PrintWriter;

/**
 * Created by bpiwowar on 17/01/16.
 */
public class Title extends Container {
    int level;

    public Title(int level, Content... contents) {
        super(contents);
        this.level = level;
    }

    @Override
    public void html(PrintWriter out) {
        out.format("<h%d>", level);
        super.html(out);
        out.format("</h%d>", level);
    }
}
