package sf.net.experimaestro.manager.js;

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

import java.io.UnsupportedEncodingException;

/**
 * A parameter file
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 24/1/13
 */
public class JSParameterFile extends JSBaseObject {
    private String key;
    private byte[] value;


    @JSFunction
    public JSParameterFile(String key, byte[] value) {
        this.setKey(key);
        this.setValue(value);
    }

    @JSFunction
    public JSParameterFile(String key, JSBaseObject object) {
        this(key, object.getBytes());
    }

    @JSFunction
    public JSParameterFile(String key, String value, String encoding) throws UnsupportedEncodingException {
        this(key, value.getBytes(encoding));
    }

    @JSFunction
    public JSParameterFile(String key, String value) throws UnsupportedEncodingException {
        this(key, value, "UTF-8");
    }


    @Override
    public String toString() {
        return getKey();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
