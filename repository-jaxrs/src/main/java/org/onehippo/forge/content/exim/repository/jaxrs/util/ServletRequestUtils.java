/*
 * Copyright 2022 Bloomreach B.V. (https://www.bloomreach.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.content.exim.repository.jaxrs.util;

import javax.servlet.http.HttpServletRequest;

public class ServletRequestUtils {

    /**
     * Default HTTP Forwarded-For header name. <code>X-Forwarded-For</code> by default.
     */
    public static final String DEFAULT_HTTP_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * Array of the default HTTP Forwarded-For header name(s). <code>{ "X-Forwarded-For" }</code> by default.
     */
    private static final String[] DEFAULT_HTTP_FORWARDED_FOR_HEADERS = { DEFAULT_HTTP_FORWARDED_FOR_HEADER };

    /**
     * Servlet context init parameter name for custom HTTP Forwarded-For header name.
     * If not set, {@link #DEFAULT_HTTP_FORWARDED_FOR_HEADER} is used by default.
     */
    public static final String HTTP_FORWARDED_FOR_HEADER_PARAM = "http-forwarded-for-header";

    /*
     * Package protected for unit tests.
     */
    static String[] httpForwardedForHeaderNames;

    private ServletRequestUtils() {
    }

    /**
     * Returns the remote host addresses related to this request.
     * If there's any proxy server between the client and the server,
     * then the proxy addresses are contained in the returned array.
     * The lowest indexed element is the farthest downstream client and
     * each successive proxy addresses are the next elements.
     * @param request servlet request
     * @return the remote host addresses related to this request
     */
    public static String[] getRemoteAddrs(HttpServletRequest request) {
        String[] headerNames = getForwardedForHeaderNames(request);

        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);

            if (headerValue != null && headerValue.length() > 0) {
                String[] addrs = headerValue.split(",");

                for (int i = 0; i < addrs.length; i++) {
                    addrs[i] = addrs[i].trim();
                }

                return addrs;
            }
        }

        return new String[] { request.getRemoteAddr() };
    }

    /**
     * Returns the remote client address.
     * @param request servlet request
     * @return the remote client address
     */
    public static String getFarthestRemoteAddr(HttpServletRequest request) {
        return getRemoteAddrs(request)[0];
    }

    /**
     * Return an array containing only <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names if {@link #HTTP_FORWARDED_FOR_HEADER_PARAM} context parameter is defined to use any other
     * comma separated custom HTTP header names instead.
     * @param request servlet request
     * @return an array containing <code>X-Forwarded-For</code> HTTP header name by default or custom equivalent
     * HTTP header names
     */
    private static String[] getForwardedForHeaderNames(final HttpServletRequest request) {
        String[] forwardedForHeaderNames = httpForwardedForHeaderNames;

        if (forwardedForHeaderNames == null) {
            synchronized (ServletRequestUtils.class) {
                forwardedForHeaderNames = httpForwardedForHeaderNames;

                if (forwardedForHeaderNames == null) {
                    String param = request.getServletContext().getInitParameter(HTTP_FORWARDED_FOR_HEADER_PARAM);

                    if (param != null && !param.isEmpty()) {
                        forwardedForHeaderNames = param.split(",");

                        for (int i = 0; i < forwardedForHeaderNames.length; i++) {
                            forwardedForHeaderNames[i] = forwardedForHeaderNames[i].trim();
                        }
                    }

                    if (forwardedForHeaderNames == null) {
                        forwardedForHeaderNames = DEFAULT_HTTP_FORWARDED_FOR_HEADERS;
                    }

                    httpForwardedForHeaderNames = forwardedForHeaderNames;
                }
            }
        }

        return forwardedForHeaderNames;
    }

}
