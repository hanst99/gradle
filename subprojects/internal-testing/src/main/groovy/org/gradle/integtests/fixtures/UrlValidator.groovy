/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.integtests.fixtures

import org.gradle.internal.hash.HashUtil
import org.gradle.util.TextUtil
import org.junit.Assert
import spock.util.concurrent.PollingConditions

class UrlValidator {

    static void available(String theUrl, String application = null, int timeout = 30) {
        URL url = new URL(theUrl)
        try {
            new PollingConditions().within(timeout) {
                assert urlIsAvailable(url)
            }
        } catch(AssertionError e) {
            throw new RuntimeException(String.format("Timeout waiting for %s to become available.", application != null ? application : theUrl));
        }
    }

    static void notAvailable(String theUrl) {
        try {
            String content = new URL(theUrl).text
            Assert.fail(String.format("Expected url '%s' to be unavailable instead we got:\n%s", theUrl, content));
        } catch (SocketException ex) {
        }
    }

    private static boolean urlIsAvailable(URL url) {
        try {
            url.text
            return true
        } catch (IOException e) {
            return false
        }
    }

    /**
     * Asserts that the content at the specified url matches the content in the provided String
     */
    static void assertUrlContentContains(URL url, String contents) {
        assert url.text.contains(contents)
    }

    /**
     * Asserts that the content at the specified url matches the content in the provided String
     */
    static void assertUrlContent(URL url, String contents) {
        assert TextUtil.normaliseLineSeparators(url.text) == TextUtil.normaliseLineSeparators(contents)
    }

    /**
     * Asserts that the content at the specified url matches the content in the specified File
     */
    static void assertUrlContent(URL url, File file) {
        assertUrlContent(url, file.text)
    }

    /**
     * Asserts that the binary content at the specified url matches the content in the specified File
     */
    static void assertBinaryUrlContent(URL url, File file) {
        assert compareHashes(url.openStream(), file.newInputStream())
    }

    private static boolean compareHashes(InputStream a, InputStream b) {
        return HashUtil.createHash(a, "MD5").equals(HashUtil.createHash(b, "MD5"))
    }
}
