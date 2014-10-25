package sf.net.experimaestro.utils;

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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

/**
 * Created by bpiwowar on 9/9/14.
 */
public class MessageDigestWriter extends Writer implements Closeable {
    private final MessageDigestStream outputStream;
    private final Charset charset;

    public MessageDigestWriter(Charset charset, String algorithm) throws NoSuchAlgorithmException {
        this.charset = charset;
        outputStream = new MessageDigestStream(algorithm);
    }

    public static byte[] stringToBytesUTFCustom(String str) {
        byte[] b = new byte[str.length() << 1];
        for (int i = 0; i < str.length(); i++) {
            char strChar = str.charAt(i);
            int bpos = i << 1;
            b[bpos] = (byte) ((strChar & 0xFF00) >> 8);
            b[bpos + 1] = (byte) (strChar & 0x00FF);
        }
        return b;
    }


    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        ByteBuffer bbuf = charset.encode(CharBuffer.wrap(cbuf, off, len));
        outputStream.write(bbuf.array());
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    public byte[] getDigest() {
        return outputStream.getDigest();
    }
}
