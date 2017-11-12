/*
 * Copyright (c) 2017 Stamina Framework developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.staminaframework.http.it;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import static io.staminaframework.runtime.starter.it.StaminaOptions.staminaDistribution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Integration tests for Stamina HTTP addon.
 *
 * @author Stamina Framework developers
 */
@RunWith(PaxExam.class)
public class HttpIT {
    private static final int port = 8090;
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() throws IOException {
        return options(
                frameworkProperty("org.apache.felix.http.host").value("localhost"),
                frameworkProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                staminaDistribution(),
                mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api").versionAsInProject().startLevel(5),
                mavenBundle("org.apache.felix", "org.apache.felix.http.api").versionAsInProject().startLevel(5),
                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty").versionAsInProject().startLevel(5)
        );
    }

    @Test
    public void testRegisterServlet() throws IOException {
        final HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/plain; charset=utf-8");

                final byte[] content = "Hello world!".getBytes("UTF-8");
                try (final OutputStream out = resp.getOutputStream()) {
                    out.write(content);
                }
            }
        };
        final Dictionary<String, Object> servletProps = new Hashtable<>(1);
        servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/helloworld");
        bundleContext.registerService(Servlet.class, servlet, servletProps);

        final URL servletUrl = createUrl("/helloworld");
        final StringBuilder resp = new StringBuilder(32);
        try (final Reader in = new InputStreamReader(new BufferedInputStream(servletUrl.openStream()))) {
            final char[] buf = new char[32];
            for (int charsRead; (charsRead = in.read(buf)) != -1; ) {
                resp.append(buf, 0, charsRead);
            }
        }

        assertTrue(resp.length() > 0);
        assertEquals("Hello world!", resp.toString());
    }

    private URL createUrl(String path) throws MalformedURLException {
        return new URL("http://localhost:" + port + path);
    }
}
