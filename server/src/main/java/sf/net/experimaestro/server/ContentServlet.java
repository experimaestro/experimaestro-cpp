package sf.net.experimaestro.server;

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

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

import static java.lang.String.format;

public class ContentServlet extends XPMServlet {
    final static private Logger LOGGER = Logger.getLogger();
    private static final long serialVersionUID = 1L;

    public ContentServlet(ServerSettings serverSettings) {
        super(serverSettings);
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        URL url = ContentServlet.class.getResource(format("/web%s",
                request.getRequestURI()));

        if (url != null) {
            Path file;
            try {
                file = Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            if (Files.isDirectory(file)) {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.setHeader("Location",
                        format("%sindex.html", request.getRequestURI()));
                return;
            }

            String filename = url.getFile();
            if (filename.endsWith(".html"))
                response.setContentType("text/html");
            else if (filename.endsWith(".png"))
                response.setContentType("image/png");
            else if (filename.endsWith(".css"))
                response.setContentType("text/css");
            response.setStatus(HttpServletResponse.SC_OK);

            final ServletOutputStream out = response.getOutputStream();
            InputStream in = url.openStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
            in.close();
            return;
        }

        // Not found
        error404(request, response);

    }

}