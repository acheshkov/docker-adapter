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
import com.artipie.docker.misc.Json;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Image manifest in JSON format.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class JsonManifest implements Manifest {

    /**
     * Manifest digest.
     */
    private final Digest dgst;

    /**
     * JSON bytes.
     */
    private final Content source;

    /**
     * Ctor.
     *
     * @param dgst Manifest digest.
     * @param source JSON bytes.
     */
    public JsonManifest(final Digest dgst, final Content source) {
        this.dgst = dgst;
        this.source = source;
    }

    @Override
    public CompletionStage<String> mediaType() {
        return this.json().thenApply(root -> root.getString("mediaType"));
    }

    @Override
    public CompletionStage<Manifest> convert(final Collection<String> options) {
        return this.mediaType().thenApply(
            type -> {
                if (!options.contains(type)) {
                    throw new IllegalArgumentException(
                        String.format("Cannot convert from '%s' to any of '%s'", type, options)
                    );
                }
                return this;
            }
        );
    }

    @Override
    public CompletionStage<Digest> config() {
        return this.json().thenApply(
            root -> new Digest.FromString(root.getJsonObject("config").getString("digest"))
        );
    }

    @Override
    public CompletionStage<Collection<Layer>> layers() {
        return this.json().thenApply(
            root -> root.getJsonArray("layers").getValuesAs(JsonValue::asJsonObject).stream()
                .map(JsonLayer::new)
                .collect(Collectors.toList())
        );
    }

    @Override
    public Digest digest() {
        return this.dgst;
    }

    @Override
    public Content content() {
        return this.source;
    }

    /**
     * Read manifest content as JSON object.
     *
     * @return JSON object.
     */
    private CompletionStage<JsonObject> json() {
        return new Json(this.source).object();
    }

    /**
     * Image layer description in JSON format.
     *
     * @since 0.2
     */
    private static final class JsonLayer implements Layer {

        /**
         * JSON object.
         */
        private final JsonObject json;

        /**
         * Ctor.
         *
         * @param json JSON object.
         */
        private JsonLayer(final JsonObject json) {
            this.json = json;
        }

        @Override
        public Digest digest() {
            return new Digest.FromString(this.json.getString("digest"));
        }

        @Override
        public Collection<URL> urls() {
            return Optional.ofNullable(this.json.getJsonArray("urls")).map(
                urls -> urls.getValuesAs(JsonString.class).stream()
                    .map(
                        str -> {
                            try {
                                return new URL(str.getString());
                            } catch (final MalformedURLException ex) {
                                throw new IllegalArgumentException(ex);
                            }
                        }
                    )
                    .collect(Collectors.toList())
            ).orElseGet(Collections::emptyList);
        }
    }
}
