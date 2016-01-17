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

import net.bpiwowar.xpm.utils.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * A definition list
 */
public class DefinitionList extends Content {
    ArrayList<Pair<Content, Content>> items = new ArrayList<>();

    public void add(Content term, Content definition) {
        items.add(Pair.of(term, definition));
    }

    @Override
    public void html(PrintWriter out) {
        if (!items.isEmpty()) {
            out.print("<dl>");
            for (Pair<Content, Content> pair : items) {
                out.print("<dt>");
                pair.getFirst().html(out);
                out.print("</dt>");
                out.print("<dd>");
                pair.getSecond().html(out);
                out.print("</dd>");
            }
        }
        out.print("</dl>");
    }

    @Override
    public void text(PrintWriter out) {
        if (!items.isEmpty()) {
            for (Pair<Content, Content> pair : items) {
                out.print("[");
                pair.getFirst().html(out);
                out.print("] ");
                pair.getSecond().html(out);
                out.println();
            }
        }
    }
}
