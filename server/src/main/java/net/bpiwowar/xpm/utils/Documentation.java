package net.bpiwowar.xpm.utils;

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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helper class when writing documentation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Documentation {
    static public abstract class Printer {
        final PrintWriter out;

        public Printer(PrintWriter out) {
            this.out = out;
        }

        public abstract void append(Content content);
    }

    static public class HTMLPrinter extends Printer {
        public HTMLPrinter(PrintWriter out) {
            super(out);
        }

        @Override
        public void append(Content content) {
            content.html(out);
        }
    }


    static public abstract class Content {
        abstract public void html(PrintWriter out);

        abstract public void text(PrintWriter out);
    }

    static public class Container extends Content {
        ArrayList<Content> contents;

        public Container(Content... contents) {
            this.contents = new ArrayList<>(Arrays.asList(contents));
        }

        public void html(PrintWriter out) {
            for (Content c : contents)
                c.html(out);
        }

        @Override
        public void text(PrintWriter out) {
            for (Content c : contents) {
                c.text(out);
            }
        }

        public void add(Content content) {
            contents.add(content);
        }
    }


    static public class Bold extends Container {

        public Bold(Content... contents) {
            super(contents);
        }

        @Override
        public void html(PrintWriter out) {
            out.print("<b>");
            super.html(out);
            out.print("</b>");
        }
    }


    static public class Title extends Container {
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

    static public class Paragraph extends Container {

        public Paragraph(Content... contents) {
            super(contents);
        }

        @Override
        public void html(PrintWriter out) {
            out.format("<p>");
            super.html(out);
            out.format("</p>%n");
        }
    }


    static public class Division extends Container {

        public Division(Content... contents) {
            super(contents);
        }

        @Override
        public void html(PrintWriter out) {
            out.format("<div>");
            super.html(out);
            out.format("</div>%n");
        }
    }

    static public class Text extends Content {
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


    static public class DefinitionList extends Content {
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
}
