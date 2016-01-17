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
public class Text extends Content {
    StringBuilder text = new StringBuilder();

    public Text() {

    }

    public Text(String text) {
        super();
        this.text.append(text);
    }

    @Override
    public void html(PrintWriter out) {
        out.print(text);
    }

    @Override
    public void text(PrintWriter out) {
        out.print(text);
    }

    public void append(String s) {
        text.append(s);
    }

    public void format(String format, Object... objects) {
        text.append(String.format(format, objects));
    }
}
