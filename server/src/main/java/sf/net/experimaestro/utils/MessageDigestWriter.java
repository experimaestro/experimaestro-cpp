package sf.net.experimaestro.utils;

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
