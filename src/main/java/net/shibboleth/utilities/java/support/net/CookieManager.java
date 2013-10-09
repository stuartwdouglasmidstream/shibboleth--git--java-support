/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.utilities.java.support.net;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A helper class for managing one or more cookies on behalf of a component.
 * 
 * <p>This bean centralizes settings related to cookie creation and access,
 * and is parametrized by name so that multiple cookies may be managed with
 * common properties.</p>
 */
public final class CookieManager extends AbstractInitializableComponent {

    /** Path of cookie. */
    @Nullable private String cookiePath;

    /** Domain of cookie. */
    @Nullable private String cookieDomain;
    
    /** Servlet request to read from. */
    @NonnullAfterInit private HttpServletRequest httpRequest;

    /** Servlet response to write to. */
    @NonnullAfterInit private HttpServletResponse httpResponse;
    
    /** Is cookie secure? */
    private boolean secure;
    
    /** Maximum age in seconds, or -1 for session. */
    private int maxAge;
    
    /** Constructor. */
    public CookieManager() {
        secure = true;
        maxAge = -1;
    }

    /**
     * Set the cookie path to use for session tracking.
     * 
     * <p>Defaults to the servlet context path.</p>
     * 
     * @param path cookie path to use, or null for the default
     */
    public void setCookiePath(@Nullable final String path) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        cookiePath = StringSupport.trimOrNull(path);
    }

    /**
     * Set the cookie domain to use for session tracking.
     * 
     * @param domain the cookie domain to use, or null for the default
     */
    public void setCookieDomain(@Nullable final String domain) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        cookieDomain = StringSupport.trimOrNull(domain);
    }

    /**
     * Set the servlet request to read from.
     * 
     * @param request servlet request
     */
    public void setHttpServletRequest(@Nonnull final HttpServletRequest request) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        httpRequest = Constraint.isNotNull(request, "HttpServletRequest cannot be null");
    }

    /**
     * Set the servlet response to write to.
     * 
     * @param response servlet response
     */
    public void setHttpServletResponse(@Nonnull final HttpServletResponse response) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        httpResponse = Constraint.isNotNull(response, "HttpServletResponse cannot be null");
    }

    /**
     * Set the SSL-only flag.
     * 
     * @param flag flag to set
     */
    public void setSecure(final boolean flag) {
        secure = flag;
    }

    /**
     * Maximum age in seconds, or -1 for per-session.
     * 
     * @param age max age to set
     */
    public void setMaxAge(final int age) {
        maxAge = age;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (httpRequest == null || httpResponse == null) {
            throw new ComponentInitializationException("Servlet request and response must be set");
        }
    }

    /**
     * Add a cookie with the specified name and value.
     * 
     * @param name  name of cookie
     * @param value value of cookie
     */
    @Nullable public void addCookie(@Nonnull @NotEmpty final String name, @Nonnull @NotEmpty final String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(cookiePath != null ? cookiePath : httpRequest.getContextPath());
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        
        httpResponse.addCookie(cookie);
    }

    /**
     * Unsets a cookie with the specified name.
     * 
     * @param name  name of cookie
     */
    @Nullable public void unsetCookie(@Nonnull @NotEmpty final String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath(cookiePath != null ? cookiePath : httpRequest.getContextPath());
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        cookie.setSecure(secure);
        cookie.setMaxAge(0);
        
        httpResponse.addCookie(cookie);
    }
}