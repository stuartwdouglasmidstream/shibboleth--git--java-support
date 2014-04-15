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
/**
 * OWASP Enterprise Security API (ESAPI)
 * 
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * <a href="http://www.owasp.org/index.php/ESAPI">http://www.owasp.org/index.php/ESAPI</a>.
 *
 * Copyright (c) 2007 - The OWASP Foundation
 * 
 * The ESAPI is published by OWASP under the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
 * 
 * @author Jeff Williams <a href="http://www.aspectsecurity.com">Aspect Security</a>
 * @created 2007
 */

package net.shibboleth.utilities.java.support.encoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An HTML encoder.
 * 
 * @see <a href="OSJ-69">https://issues.shibboleth.net/jira/browse/OSJ-69</a>
 * 
 * @see org.owasp.esapi.Encoder
 * @see org.owasp.esapi.reference.DefaultEncoder
 * @see org.owasp.esapi.codecs.HTMLEntityCodec
 */
public final class HTMLEncoder {

    /** Character set immune from HTML encoding. */
    @Nonnull public static final char[] IMMUNE_HTML = {',', '.', '-', '_', ' '};

    /** Character set immune from HTML attribute encoding. */
    @Nonnull public static final char[] IMMUNE_HTMLATTR = {',', '.', '-', '_'};

    /** Character to replace illegal characters. */
    @Nonnull public static final char REPLACEMENT_CHAR = '\ufffd';

    /** Hex to replace illegal characters. */
    @Nonnull public static final String REPLACEMENT_HEX = "fffd";

    /** Map from entity character to name. */
    @Nonnull private static final Map<Character, String> CHARACTER_TO_ENTITY_MAP = mkCharacterToEntityMap();

    /**
     * Initialize an array to mark which characters are to be encoded. Store the hex string for that character to save
     * time later. If the character shouldn't be encoded, then store null.
     */
    private static final String[] HEX = new String[256];

    static {
        for (char c = 0; c < 0xFF; c++) {
            if (c >= 0x30 && c <= 0x39 || c >= 0x41 && c <= 0x5A || c >= 0x61 && c <= 0x7A) {
                HEX[c] = null;
            } else {
                HEX[c] = Integer.toHexString(c).intern();
            }
        }
    }

    /** Constructor. */
    private HTMLEncoder() {

    }

    /**
     * Encode data for use in HTML using HTML entity encoding
     * <p>
     * Note that the following characters: 00-08, 0B-0C, 0E-1F, and 7F-9F
     * <p>
     * cannot be used in HTML.
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Character_encodings_in_HTML">HTML Encodings [wikipedia.org]</a>
     * @see <a href="http://www.w3.org/TR/html4/sgml/sgmldecl.html">SGML Specification [w3.org]</a>
     * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">XML Specification [w3.org]</a>
     * 
     * @param input the text to encode for HTML
     * 
     * @return input encoded for HTML
     */
    @Nullable public static String encodeForHTML(@Nullable final String input) {
        if (input == null) {
            return null;
        }
        return encode(IMMUNE_HTML, input);
    }

    /**
     * Encode data for use in HTML attributes.
     * 
     * @param input the text to encode for an HTML attribute
     * 
     * @return input encoded for use as an HTML attribute
     */
    @Nullable public static String encodeForHTMLAttribute(@Nullable final String input) {
        if (input == null) {
            return null;
        }
        return encode(IMMUNE_HTMLATTR, input);
    }

    /**
     * Encode a string for safe use in an HTML entity field.
     * 
     * @param immune characters immune from encoding
     * @param input the string to encode
     * @return the encoded input
     */
    @Nonnull private static String encode(@Nonnull final char[] immune, @Nonnull final String input) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            sb.append(encodeCharacter(immune, c));
        }
        return sb.toString();
    }

    /**
     * Encodes a character for safe use in an HTML entity field.
     * 
     * @param immune characters immune from encoding
     * @param toEncode the character to encode
     * @return the encoded character
     */
    @Nonnull private static String encodeCharacter(@Nonnull final char[] immune, @Nonnull final Character toEncode) {

        Character c = toEncode;

        // check for immune characters
        if (containsCharacter(c, immune)) {
            return "" + c;
        }

        // check for alphanumeric characters
        String hex = getHexForNonAlphanumeric(c);
        if (hex == null) {
            return "" + c;
        }

        // check for illegal characters
        if ((c <= 0x1f && c != '\t' && c != '\n' && c != '\r') || (c >= 0x7f && c <= 0x9f)) {
            // Let's entity encode this instead of returning it
            hex = REPLACEMENT_HEX;
            c = REPLACEMENT_CHAR;
        }

        // check if there's a defined entity
        final String entityName = CHARACTER_TO_ENTITY_MAP.get(c);
        if (entityName != null) {
            return "&" + entityName + ";";
        }

        // return the hex entity as suggested in the spec
        return "&#x" + hex + ";";
    }

    /**
     * Lookup the hex value of any character that is not alphanumeric.
     * 
     * @param c the character to lookup.
     * @return null if alphanumeric or the character code in hex.
     */
    @Nonnull private static String getHexForNonAlphanumeric(@Nonnull final char c) {
        if (c < 0xFF) {
            return HEX[c];
        }
        return Integer.toHexString(c);
    }

    /**
     * Utility to search a char[] for a specific char.
     * 
     * @param c the char
     * @param array the array
     * @return whether or not the array contains the char
     */
    private static boolean containsCharacter(@Nonnull final char c, @Nonnull final char[] array) {
        for (char ch : array) {
            if (c == ch) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build an unmodifiable map from entity character to name.
     * 
     * @return unmodifiable map from entity character to name
     */
    // Checkstyle: MethodLength|TrailingComment OFF -- copied from OWASP ESAPI.
    @Nonnull private static synchronized Map<Character, String> mkCharacterToEntityMap() {
        final Map<Character, String> map = new HashMap<Character, String>(252);

        map.put((char) 34, "quot"); /* quotation mark */
        map.put((char) 38, "amp"); /* ampersand */
        map.put((char) 60, "lt"); /* less-than sign */
        map.put((char) 62, "gt"); /* greater-than sign */
        map.put((char) 160, "nbsp"); /* no-break space */
        map.put((char) 161, "iexcl"); /* inverted exclamation mark */
        map.put((char) 162, "cent"); /* cent sign */
        map.put((char) 163, "pound"); /* pound sign */
        map.put((char) 164, "curren"); /* currency sign */
        map.put((char) 165, "yen"); /* yen sign */
        map.put((char) 166, "brvbar"); /* broken bar */
        map.put((char) 167, "sect"); /* section sign */
        map.put((char) 168, "uml"); /* diaeresis */
        map.put((char) 169, "copy"); /* copyright sign */
        map.put((char) 170, "ordf"); /* feminine ordinal indicator */
        map.put((char) 171, "laquo"); /* left-pointing double angle quotation mark */
        map.put((char) 172, "not"); /* not sign */
        map.put((char) 173, "shy"); /* soft hyphen */
        map.put((char) 174, "reg"); /* registered sign */
        map.put((char) 175, "macr"); /* macron */
        map.put((char) 176, "deg"); /* degree sign */
        map.put((char) 177, "plusmn"); /* plus-minus sign */
        map.put((char) 178, "sup2"); /* superscript two */
        map.put((char) 179, "sup3"); /* superscript three */
        map.put((char) 180, "acute"); /* acute accent */
        map.put((char) 181, "micro"); /* micro sign */
        map.put((char) 182, "para"); /* pilcrow sign */
        map.put((char) 183, "middot"); /* middle dot */
        map.put((char) 184, "cedil"); /* cedilla */
        map.put((char) 185, "sup1"); /* superscript one */
        map.put((char) 186, "ordm"); /* masculine ordinal indicator */
        map.put((char) 187, "raquo"); /* right-pointing double angle quotation mark */
        map.put((char) 188, "frac14"); /* vulgar fraction one quarter */
        map.put((char) 189, "frac12"); /* vulgar fraction one half */
        map.put((char) 190, "frac34"); /* vulgar fraction three quarters */
        map.put((char) 191, "iquest"); /* inverted question mark */
        map.put((char) 192, "Agrave"); /* Latin capital letter a with grave */
        map.put((char) 193, "Aacute"); /* Latin capital letter a with acute */
        map.put((char) 194, "Acirc"); /* Latin capital letter a with circumflex */
        map.put((char) 195, "Atilde"); /* Latin capital letter a with tilde */
        map.put((char) 196, "Auml"); /* Latin capital letter a with diaeresis */
        map.put((char) 197, "Aring"); /* Latin capital letter a with ring above */
        map.put((char) 198, "AElig"); /* Latin capital letter ae */
        map.put((char) 199, "Ccedil"); /* Latin capital letter c with cedilla */
        map.put((char) 200, "Egrave"); /* Latin capital letter e with grave */
        map.put((char) 201, "Eacute"); /* Latin capital letter e with acute */
        map.put((char) 202, "Ecirc"); /* Latin capital letter e with circumflex */
        map.put((char) 203, "Euml"); /* Latin capital letter e with diaeresis */
        map.put((char) 204, "Igrave"); /* Latin capital letter i with grave */
        map.put((char) 205, "Iacute"); /* Latin capital letter i with acute */
        map.put((char) 206, "Icirc"); /* Latin capital letter i with circumflex */
        map.put((char) 207, "Iuml"); /* Latin capital letter i with diaeresis */
        map.put((char) 208, "ETH"); /* Latin capital letter eth */
        map.put((char) 209, "Ntilde"); /* Latin capital letter n with tilde */
        map.put((char) 210, "Ograve"); /* Latin capital letter o with grave */
        map.put((char) 211, "Oacute"); /* Latin capital letter o with acute */
        map.put((char) 212, "Ocirc"); /* Latin capital letter o with circumflex */
        map.put((char) 213, "Otilde"); /* Latin capital letter o with tilde */
        map.put((char) 214, "Ouml"); /* Latin capital letter o with diaeresis */
        map.put((char) 215, "times"); /* multiplication sign */
        map.put((char) 216, "Oslash"); /* Latin capital letter o with stroke */
        map.put((char) 217, "Ugrave"); /* Latin capital letter u with grave */
        map.put((char) 218, "Uacute"); /* Latin capital letter u with acute */
        map.put((char) 219, "Ucirc"); /* Latin capital letter u with circumflex */
        map.put((char) 220, "Uuml"); /* Latin capital letter u with diaeresis */
        map.put((char) 221, "Yacute"); /* Latin capital letter y with acute */
        map.put((char) 222, "THORN"); /* Latin capital letter thorn */
        map.put((char) 223, "szlig"); /* Latin small letter sharp sXCOMMAX German Eszett */
        map.put((char) 224, "agrave"); /* Latin small letter a with grave */
        map.put((char) 225, "aacute"); /* Latin small letter a with acute */
        map.put((char) 226, "acirc"); /* Latin small letter a with circumflex */
        map.put((char) 227, "atilde"); /* Latin small letter a with tilde */
        map.put((char) 228, "auml"); /* Latin small letter a with diaeresis */
        map.put((char) 229, "aring"); /* Latin small letter a with ring above */
        map.put((char) 230, "aelig"); /* Latin lowercase ligature ae */
        map.put((char) 231, "ccedil"); /* Latin small letter c with cedilla */
        map.put((char) 232, "egrave"); /* Latin small letter e with grave */
        map.put((char) 233, "eacute"); /* Latin small letter e with acute */
        map.put((char) 234, "ecirc"); /* Latin small letter e with circumflex */
        map.put((char) 235, "euml"); /* Latin small letter e with diaeresis */
        map.put((char) 236, "igrave"); /* Latin small letter i with grave */
        map.put((char) 237, "iacute"); /* Latin small letter i with acute */
        map.put((char) 238, "icirc"); /* Latin small letter i with circumflex */
        map.put((char) 239, "iuml"); /* Latin small letter i with diaeresis */
        map.put((char) 240, "eth"); /* Latin small letter eth */
        map.put((char) 241, "ntilde"); /* Latin small letter n with tilde */
        map.put((char) 242, "ograve"); /* Latin small letter o with grave */
        map.put((char) 243, "oacute"); /* Latin small letter o with acute */
        map.put((char) 244, "ocirc"); /* Latin small letter o with circumflex */
        map.put((char) 245, "otilde"); /* Latin small letter o with tilde */
        map.put((char) 246, "ouml"); /* Latin small letter o with diaeresis */
        map.put((char) 247, "divide"); /* division sign */
        map.put((char) 248, "oslash"); /* Latin small letter o with stroke */
        map.put((char) 249, "ugrave"); /* Latin small letter u with grave */
        map.put((char) 250, "uacute"); /* Latin small letter u with acute */
        map.put((char) 251, "ucirc"); /* Latin small letter u with circumflex */
        map.put((char) 252, "uuml"); /* Latin small letter u with diaeresis */
        map.put((char) 253, "yacute"); /* Latin small letter y with acute */
        map.put((char) 254, "thorn"); /* Latin small letter thorn */
        map.put((char) 255, "yuml"); /* Latin small letter y with diaeresis */
        map.put((char) 338, "OElig"); /* Latin capital ligature oe */
        map.put((char) 339, "oelig"); /* Latin small ligature oe */
        map.put((char) 352, "Scaron"); /* Latin capital letter s with caron */
        map.put((char) 353, "scaron"); /* Latin small letter s with caron */
        map.put((char) 376, "Yuml"); /* Latin capital letter y with diaeresis */
        map.put((char) 402, "fnof"); /* Latin small letter f with hook */
        map.put((char) 710, "circ"); /* modifier letter circumflex accent */
        map.put((char) 732, "tilde"); /* small tilde */
        map.put((char) 913, "Alpha"); /* Greek capital letter alpha */
        map.put((char) 914, "Beta"); /* Greek capital letter beta */
        map.put((char) 915, "Gamma"); /* Greek capital letter gamma */
        map.put((char) 916, "Delta"); /* Greek capital letter delta */
        map.put((char) 917, "Epsilon"); /* Greek capital letter epsilon */
        map.put((char) 918, "Zeta"); /* Greek capital letter zeta */
        map.put((char) 919, "Eta"); /* Greek capital letter eta */
        map.put((char) 920, "Theta"); /* Greek capital letter theta */
        map.put((char) 921, "Iota"); /* Greek capital letter iota */
        map.put((char) 922, "Kappa"); /* Greek capital letter kappa */
        map.put((char) 923, "Lambda"); /* Greek capital letter lambda */
        map.put((char) 924, "Mu"); /* Greek capital letter mu */
        map.put((char) 925, "Nu"); /* Greek capital letter nu */
        map.put((char) 926, "Xi"); /* Greek capital letter xi */
        map.put((char) 927, "Omicron"); /* Greek capital letter omicron */
        map.put((char) 928, "Pi"); /* Greek capital letter pi */
        map.put((char) 929, "Rho"); /* Greek capital letter rho */
        map.put((char) 931, "Sigma"); /* Greek capital letter sigma */
        map.put((char) 932, "Tau"); /* Greek capital letter tau */
        map.put((char) 933, "Upsilon"); /* Greek capital letter upsilon */
        map.put((char) 934, "Phi"); /* Greek capital letter phi */
        map.put((char) 935, "Chi"); /* Greek capital letter chi */
        map.put((char) 936, "Psi"); /* Greek capital letter psi */
        map.put((char) 937, "Omega"); /* Greek capital letter omega */
        map.put((char) 945, "alpha"); /* Greek small letter alpha */
        map.put((char) 946, "beta"); /* Greek small letter beta */
        map.put((char) 947, "gamma"); /* Greek small letter gamma */
        map.put((char) 948, "delta"); /* Greek small letter delta */
        map.put((char) 949, "epsilon"); /* Greek small letter epsilon */
        map.put((char) 950, "zeta"); /* Greek small letter zeta */
        map.put((char) 951, "eta"); /* Greek small letter eta */
        map.put((char) 952, "theta"); /* Greek small letter theta */
        map.put((char) 953, "iota"); /* Greek small letter iota */
        map.put((char) 954, "kappa"); /* Greek small letter kappa */
        map.put((char) 955, "lambda"); /* Greek small letter lambda */
        map.put((char) 956, "mu"); /* Greek small letter mu */
        map.put((char) 957, "nu"); /* Greek small letter nu */
        map.put((char) 958, "xi"); /* Greek small letter xi */
        map.put((char) 959, "omicron"); /* Greek small letter omicron */
        map.put((char) 960, "pi"); /* Greek small letter pi */
        map.put((char) 961, "rho"); /* Greek small letter rho */
        map.put((char) 962, "sigmaf"); /* Greek small letter final sigma */
        map.put((char) 963, "sigma"); /* Greek small letter sigma */
        map.put((char) 964, "tau"); /* Greek small letter tau */
        map.put((char) 965, "upsilon"); /* Greek small letter upsilon */
        map.put((char) 966, "phi"); /* Greek small letter phi */
        map.put((char) 967, "chi"); /* Greek small letter chi */
        map.put((char) 968, "psi"); /* Greek small letter psi */
        map.put((char) 969, "omega"); /* Greek small letter omega */
        map.put((char) 977, "thetasym"); /* Greek theta symbol */
        map.put((char) 978, "upsih"); /* Greek upsilon with hook symbol */
        map.put((char) 982, "piv"); /* Greek pi symbol */
        map.put((char) 8194, "ensp"); /* en space */
        map.put((char) 8195, "emsp"); /* em space */
        map.put((char) 8201, "thinsp"); /* thin space */
        map.put((char) 8204, "zwnj"); /* zero width non-joiner */
        map.put((char) 8205, "zwj"); /* zero width joiner */
        map.put((char) 8206, "lrm"); /* left-to-right mark */
        map.put((char) 8207, "rlm"); /* right-to-left mark */
        map.put((char) 8211, "ndash"); /* en dash */
        map.put((char) 8212, "mdash"); /* em dash */
        map.put((char) 8216, "lsquo"); /* left single quotation mark */
        map.put((char) 8217, "rsquo"); /* right single quotation mark */
        map.put((char) 8218, "sbquo"); /* single low-9 quotation mark */
        map.put((char) 8220, "ldquo"); /* left double quotation mark */
        map.put((char) 8221, "rdquo"); /* right double quotation mark */
        map.put((char) 8222, "bdquo"); /* double low-9 quotation mark */
        map.put((char) 8224, "dagger"); /* dagger */
        map.put((char) 8225, "Dagger"); /* double dagger */
        map.put((char) 8226, "bull"); /* bullet */
        map.put((char) 8230, "hellip"); /* horizontal ellipsis */
        map.put((char) 8240, "permil"); /* per mille sign */
        map.put((char) 8242, "prime"); /* prime */
        map.put((char) 8243, "Prime"); /* double prime */
        map.put((char) 8249, "lsaquo"); /* single left-pointing angle quotation mark */
        map.put((char) 8250, "rsaquo"); /* single right-pointing angle quotation mark */
        map.put((char) 8254, "oline"); /* overline */
        map.put((char) 8260, "frasl"); /* fraction slash */
        map.put((char) 8364, "euro"); /* euro sign */
        map.put((char) 8465, "image"); /* black-letter capital i */
        map.put((char) 8472, "weierp"); /* script capital pXCOMMAX Weierstrass p */
        map.put((char) 8476, "real"); /* black-letter capital r */
        map.put((char) 8482, "trade"); /* trademark sign */
        map.put((char) 8501, "alefsym"); /* alef symbol */
        map.put((char) 8592, "larr"); /* leftwards arrow */
        map.put((char) 8593, "uarr"); /* upwards arrow */
        map.put((char) 8594, "rarr"); /* rightwards arrow */
        map.put((char) 8595, "darr"); /* downwards arrow */
        map.put((char) 8596, "harr"); /* left right arrow */
        map.put((char) 8629, "crarr"); /* downwards arrow with corner leftwards */
        map.put((char) 8656, "lArr"); /* leftwards double arrow */
        map.put((char) 8657, "uArr"); /* upwards double arrow */
        map.put((char) 8658, "rArr"); /* rightwards double arrow */
        map.put((char) 8659, "dArr"); /* downwards double arrow */
        map.put((char) 8660, "hArr"); /* left right double arrow */
        map.put((char) 8704, "forall"); /* for all */
        map.put((char) 8706, "part"); /* partial differential */
        map.put((char) 8707, "exist"); /* there exists */
        map.put((char) 8709, "empty"); /* empty set */
        map.put((char) 8711, "nabla"); /* nabla */
        map.put((char) 8712, "isin"); /* element of */
        map.put((char) 8713, "notin"); /* not an element of */
        map.put((char) 8715, "ni"); /* contains as member */
        map.put((char) 8719, "prod"); /* n-ary product */
        map.put((char) 8721, "sum"); /* n-ary summation */
        map.put((char) 8722, "minus"); /* minus sign */
        map.put((char) 8727, "lowast"); /* asterisk operator */
        map.put((char) 8730, "radic"); /* square root */
        map.put((char) 8733, "prop"); /* proportional to */
        map.put((char) 8734, "infin"); /* infinity */
        map.put((char) 8736, "ang"); /* angle */
        map.put((char) 8743, "and"); /* logical and */
        map.put((char) 8744, "or"); /* logical or */
        map.put((char) 8745, "cap"); /* intersection */
        map.put((char) 8746, "cup"); /* union */
        map.put((char) 8747, "int"); /* integral */
        map.put((char) 8756, "there4"); /* therefore */
        map.put((char) 8764, "sim"); /* tilde operator */
        map.put((char) 8773, "cong"); /* congruent to */
        map.put((char) 8776, "asymp"); /* almost equal to */
        map.put((char) 8800, "ne"); /* not equal to */
        map.put((char) 8801, "equiv"); /* identical toXCOMMAX equivalent to */
        map.put((char) 8804, "le"); /* less-than or equal to */
        map.put((char) 8805, "ge"); /* greater-than or equal to */
        map.put((char) 8834, "sub"); /* subset of */
        map.put((char) 8835, "sup"); /* superset of */
        map.put((char) 8836, "nsub"); /* not a subset of */
        map.put((char) 8838, "sube"); /* subset of or equal to */
        map.put((char) 8839, "supe"); /* superset of or equal to */
        map.put((char) 8853, "oplus"); /* circled plus */
        map.put((char) 8855, "otimes"); /* circled times */
        map.put((char) 8869, "perp"); /* up tack */
        map.put((char) 8901, "sdot"); /* dot operator */
        map.put((char) 8968, "lceil"); /* left ceiling */
        map.put((char) 8969, "rceil"); /* right ceiling */
        map.put((char) 8970, "lfloor"); /* left floor */
        map.put((char) 8971, "rfloor"); /* right floor */
        map.put((char) 9001, "lang"); /* left-pointing angle bracket */
        map.put((char) 9002, "rang"); /* right-pointing angle bracket */
        map.put((char) 9674, "loz"); /* lozenge */
        map.put((char) 9824, "spades"); /* black spade suit */
        map.put((char) 9827, "clubs"); /* black club suit */
        map.put((char) 9829, "hearts"); /* black heart suit */
        map.put((char) 9830, "diams"); /* black diamond suit */

        // Checkstyle: MethodLength|TrailingComment ON

        return Collections.unmodifiableMap(map);
    }

}
