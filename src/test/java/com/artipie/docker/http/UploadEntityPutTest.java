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
package com.artipie.docker.http;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.BlobKey;
import com.artipie.http.Response;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Upload PUT endpoint.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UploadEntityPutTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Docker registry used in tests.
     */
    private Docker docker;

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.docker = new AstoDocker(this.storage);
        this.slice = new DockerSlice("/base", this.docker);
    }

    @Test
    void shouldFinishUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(Flowable.just(ByteBuffer.wrap("data".getBytes())))
            .toCompletableFuture().join();
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"
        );
        final Response response = this.slice.response(
            UploadEntityPutTest.requestLine(name, upload.uuid(), digest),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Returns 201 status and corresponding headers",
            response,
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.CREATED),
                    new RsHasHeaders(
                        new Header("Location", String.format("/v2/%s/blobs/%s", name, digest)),
                        new Header("Content-Length", "0"),
                        new Header("Docker-Content-Digest", digest)
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Puts blob into storage",
            this.storage.exists(new BlobKey(new Digest.FromString(digest))).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsBadRequestWhenDigestsDoNotMatch() {
        final String name = "repo";
        final byte[] content = "something".getBytes();
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads().start()
            .toCompletableFuture().join();
        upload.append(Flowable.just(ByteBuffer.wrap(content)))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Returns 400 status",
            this.slice.response(
                UploadEntityPutTest.requestLine(name, upload.uuid(), "sha256:0000"),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
        MatcherAssert.assertThat(
            "Does not put blob into storage",
            this.storage.exists(
                new BlobKey(new Digest.Sha256(content))
            ).join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine("PUT", "/base/v2/test/blobs/uploads/12345", "HTTP/1.1").toString(),
            Collections.emptyList(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    /**
     * Returns request line.
     * @param name Repo name
     * @param uuid Upload uuid
     * @param digest Digest
     * @return RequestLine instance
     */
    private static String requestLine(final String name, final String uuid, final String digest) {
        return new RequestLine(
            "PUT",
            String.format("/base/v2/%s/blobs/uploads/%s?digest=%s", name, uuid, digest),
            "HTTP/1.1"
        ).toString();
    }

}
