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
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.misc.ByteBufPublisher;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonManifest}.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JsonManifestTest {

    @Test
    void shouldReadMediaType() throws Exception {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From("{\"mediaType\":\"something\"}".getBytes())
        );
        MatcherAssert.assertThat(
            manifest.mediaType().toCompletableFuture().get(),
            new IsEqual<>("something")
        );
    }

    @Test
    void shouldConvertToSameType() throws Exception {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From("{\"mediaType\":\"type2\"}".getBytes())
        );
        MatcherAssert.assertThat(
            manifest.convert(Arrays.asList("type1", "type2")).toCompletableFuture().get(),
            new IsEqual<>(manifest)
        );
    }

    @Test
    void shouldFailConvertToUnknownType() {
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From("{\"mediaType\":\"typeA\"}".getBytes())
        );
        final ExecutionException exception = Assertions.assertThrows(
            ExecutionException.class,
            () -> manifest.convert(Collections.singleton("typeB")).toCompletableFuture().get()
        );
        MatcherAssert.assertThat(
            exception.getCause(),
            new IsInstanceOf(IllegalArgumentException.class)
        );
    }

    @Test
    void shouldReadConfig() {
        final String digest = "sha256:def";
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From(
                Json.createObjectBuilder().add(
                    "config",
                    Json.createObjectBuilder().add("digest", digest)
                ).build().toString().getBytes()
            )
        );
        MatcherAssert.assertThat(
            manifest.config().toCompletableFuture().join().string(),
            new IsEqual<>(digest)
        );
    }

    @Test
    void shouldReadLayerDigests() {
        final String[] digests = {"sha256:123", "sha256:abc"};
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("12345"),
            new Content.From(
                Json.createObjectBuilder().add(
                    "layers",
                    Json.createArrayBuilder(
                        Stream.of(digests)
                            .map(dig -> Collections.singletonMap("digest", dig))
                            .collect(Collectors.toList())
                    )
                ).build().toString().getBytes()
            )
        );
        MatcherAssert.assertThat(
            manifest.layers().toCompletableFuture().join().stream()
                .map(Layer::digest)
                .map(Digest::string)
                .collect(Collectors.toList()),
            Matchers.containsInAnyOrder(digests)
        );
    }

    @Test
    void shouldReadLayerUrls() throws Exception {
        final String url = "https://artipie.com/";
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From(
                Json.createObjectBuilder().add(
                    "layers",
                    Json.createArrayBuilder().add(
                        Json.createObjectBuilder()
                            .add("digest", "sha256:12345")
                            .add(
                                "urls",
                                Json.createArrayBuilder().add(url)
                            )
                    )
                ).build().toString().getBytes()
            )
        );
        MatcherAssert.assertThat(
            manifest.layers().toCompletableFuture().join().stream()
                .flatMap(layer -> layer.urls().stream())
                .collect(Collectors.toList()),
            new IsIterableContaining<>(new IsEqual<>(new URL(url)))
        );
    }

    @Test
    void shouldReadDigest() {
        final String digest = "sha256:123";
        final JsonManifest manifest = new JsonManifest(
            new Digest.FromString(digest),
            new Content.From("something".getBytes())
        );
        MatcherAssert.assertThat(
            manifest.digest().string(),
            new IsEqual<>(digest)
        );
    }

    @Test
    void shouldReadContent() {
        final byte[] data = "data".getBytes();
        final JsonManifest manifest = new JsonManifest(
            new Digest.Sha256("123"),
            new Content.From(data)
        );
        MatcherAssert.assertThat(
            new ByteBufPublisher(manifest.content()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
    }
}
