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

package net.bpiwowar.xpm.manager;

import com.google.common.collect.ImmutableList;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.MessageDigestWriter;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

/**
 * Unique path given a JSON
 */
public class UniquePath {
    private final Path uniquePath;
    private final String descriptor;
    private final boolean directory;

    private static String getDigest(String string) throws NoSuchAlgorithmException, IOException {
        MessageDigestWriter writer = new MessageDigestWriter(Charset.forName("UTF-8"), "MD5");
        writer.write(string);
        writer.close();

        return DatatypeConverter.printHexBinary(writer.getDigest()).toLowerCase();
    }

    public static String getDescriptor(Json json) throws IOException {
        StringWriter writer = new StringWriter();
        json.writeDescriptorString(writer);
        return writer.getBuffer().toString();
    }

    /**
     * Get the hash of a given json
     */
    public static String getDigest(Json json) throws NoSuchAlgorithmException, IOException {
        // Other options: SHA-256, SHA-512
        MessageDigestWriter writer = new MessageDigestWriter(Charset.forName("UTF-8"), "MD5");
        json.writeDescriptorString(writer);
        writer.close();

        return DatatypeConverter.printHexBinary(writer.getDigest()).toLowerCase();
    }

    public UniquePath create() throws IOException {
        Path signature = directory ? uniquePath.resolve(Constants.XPM_SIGNATURE) : uniquePath;
        Files.createDirectories(uniquePath);
        Files.write(signature, ImmutableList.of(this.descriptor));
        return this;
    }

    public Path getUniquePath() {
        return uniquePath;
    }

    public UniquePath(Path basedir, String prefix, QName id, Json jsonValues, boolean directory) throws IOException, NoSuchAlgorithmException {
        // Create JSON object, get the description JSON and digest
        JsonObject json = new JsonObject();
        json.put("task", id.toString());
        json.put("value", jsonValues);

        descriptor = getDescriptor(json);
        String digest = getDigest(descriptor);

        if (basedir == null) {
            basedir = ScriptContext.get().getWorkingDirectory();
        }
        uniquePath = basedir.resolve(format("%s/%s", prefix, digest));
        this.directory = directory;

        // Verify the signature if the directory exists

        Path signature = directory ? uniquePath.resolve(Constants.XPM_SIGNATURE) : uniquePath;
        if (Files.exists(signature)) {
            if (!Files.isRegularFile(signature)) {
                throw new XPMRhinoException("Path %s exists and is not a file", signature);
            }
            // Check that the signature is the same
            // @TODO more efficient comparison by avoiding to compute the whole signature
            char buffer[] = new char[1024];
            int offset = 0;
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(signature))) {
                int read;
                while ((read = reader.read(buffer)) > 0) {
                    if (offset + read > descriptor.length()) {
                        throw new RuntimeException("Signature JSON do not match in " + signature.toString());
                    }

                    for (int i = 0; i < read; ++i) {
                        if (buffer[i] != descriptor.charAt(offset + i)) {
                            throw new XPMRuntimeException("Signature JSON do not match in %s: at offset %d, %s <> %s",
                                    signature.toString(), i + offset, buffer[i], descriptor.charAt(i + offset));
                        }
                    }

                    offset += read;

                }
            }
        }
    }
}
