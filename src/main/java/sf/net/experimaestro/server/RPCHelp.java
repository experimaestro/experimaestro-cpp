/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Help
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/10/12
 */
@Retention(RetentionPolicy.RUNTIME)
@interface RPCHelp {
    /** Help message */
    String value();

    /** Parameters documentation */
    String [] parameters() default {};
}
