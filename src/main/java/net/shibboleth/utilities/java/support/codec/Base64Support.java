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

package net.shibboleth.utilities.java.support.codec;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.apache.commons.codec.binary.Base64;

/**
 * Helper class for working with {@link Base64}.
 * 
 * <p>
 * This helper class specifically addresses that waste of the Apache Codec encode/decode static methods creating new
 * instances of the {@link Base64} for every operation. It also provides the helper method to produce both chunked and
 * unchunked encoded content as strings.
 * </p>
 */
public final class Base64Support {

    /** Chunk the encoded data into 76-character lines broken by CRLF characters. */
    public static final boolean CHUNKED = true;

    /** Do not chunk encoded data. */
    public static final boolean UNCHUNKED = false;

    /** Encoder used to produce chunked output. */
    @Nonnull private static final Base64 CHUNKED_ENCODER = new Base64(76, new byte[] { '\n' });

    /** Encoder used to produce unchunked output. */
    @Nonnull private static final Base64 UNCHUNKED_ENCODER = new Base64(0, new byte[] { '\n' });

    /** Constructor. */
    private Base64Support() {

    }

    /**
     * Base64 encodes the given binary data.
     * 
     * @param data data to encode
     * @param chunked whether the encoded data should be chunked or not
     * 
     * @return the base64 encoded data
     */
    @Nonnull public static String encode(@Nonnull final byte[] data, final boolean chunked) {
        Constraint.isNotNull(data, "Binary data to be encoded can not be null");
        if (chunked) {
            return Constraint.isNotNull(StringSupport.trim(CHUNKED_ENCODER.encodeToString(data)),
                    "Encoded data was null");
        } else {
            return Constraint.isNotNull(StringSupport.trim(UNCHUNKED_ENCODER.encodeToString(data)),
                    "Encoded data was null");
        }
    }

    /**
     * Decodes (un)chunked Base64 encoded data.
     * 
     * @param data Base64 encoded data
     * 
     * @return the decoded data
     */
    @Nonnull public static byte[] decode(@Nonnull final String data) {
        Constraint.isNotNull(data, "Base64 encoded data can not be null");
        return Constraint.isNotNull(CHUNKED_ENCODER.decode(data), "Decoded data was null");
    }
}