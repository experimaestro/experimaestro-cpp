package sf.net.experimaestro.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wraps the MessageDigest
 *
 * Created by bpiwowar on 9/9/14.
 */
public class MessageDigestStream extends OutputStream {
    private final MessageDigest messageDigest;

    public MessageDigestStream(String algorithm) throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance(algorithm);
    }

    @Override
    public void write(int b) throws IOException {
        messageDigest.update((byte)(b& (0xff)));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        messageDigest.update(b, off, len);
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    public byte[] getDigest() {
        return messageDigest.digest();
    }
}
