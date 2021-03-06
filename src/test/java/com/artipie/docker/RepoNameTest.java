/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.docker;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RepoName}.
 * @since 0.1
 */
final class RepoNameTest {
    @Test
    void acceptsValidRepoName() {
        MatcherAssert.assertThat(
            new RepoName.Valid("ab/c/0/x-y/c_z/v.p/qqqqqqqqqqqqqqqqqqqqqqq").value(),
            Matchers.not(Matchers.blankOrNullString())
        );
    }

    @Test
    void cannotBeEmpty() {
        Assertions.assertThrows(IllegalStateException.class, () -> new RepoName.Valid("").value());
    }

    @Test
    void cannotBeGreaterThanMaxLength() {
        Assertions.assertThrows(
            IllegalStateException.class,
            // @checkstyle MagicNumberCheck (1 line)
            () -> new RepoName.Valid(RepoNameTest.repeatChar('a', 256)).value()
        );
    }

    @Test
    void cannotEndWithSlash() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RepoName.Valid("asd/").value()
        );
    }

    @Test
    void cannotIncludeStrangeSymbols() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RepoName.Valid("asd+zxc").value()
        );
    }

    /**
     * Generates new string with repeated char.
     * @param chr Char to repeat
     * @param count String size
     */
    private static String repeatChar(final char chr, final int count) {
        final StringBuilder str = new StringBuilder(count);
        for (int pos = 0; pos < count; ++pos) {
            str.append(chr);
        }
        return str.toString();
    }
}
