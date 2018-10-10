/*

Parts of this implementations are copied from com.google.protobuf.ByteString.

Because of that we attach Google Protocol Buffers redistribution notice:

    Copyright 2014, Google Inc.  All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:

        * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following disclaimer
    in the documentation and/or other materials provided with the
    distribution.
        * Neither the name of Google Inc. nor the names of its
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
    OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.avast.bytes.internal;

import com.avast.bytes.Bytes;
import com.avast.bytes.ConcatBytes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * This is a utility class that is not part of the public API of the Bytes library.
 */
public class StreamReader {

    /**
     * When copying an InputStream into a Bytes with .readFrom(),
     * the chunks in the underlying ConcatBytes start at 256 bytes, but double
     * each iteration up to 8192 bytes.
     */
    private static final int MIN_READ_FROM_CHUNK_SIZE = 0x100;  // 256b
    private static final int MAX_READ_FROM_CHUNK_SIZE = 0x2000;  // 8k

    public static Bytes readFrom(InputStream streamToDrain, IntFunction<Bytes.BuilderStream> builderFactory) throws IOException {
        return readFrom(streamToDrain, MIN_READ_FROM_CHUNK_SIZE, MAX_READ_FROM_CHUNK_SIZE, builderFactory);
    }

    public static Bytes readSliceFrom(InputStream streamToDrain, int offset, int len, IntFunction<Bytes.BuilderStream> builderFactory) throws IOException {
        // the implementation is based on commons-io IOUtils.copyLarge

        if (len == 0) {
            return Bytes.empty();
        }

        if (offset > 0) {
            final long skipped = streamToDrain.skip(offset);
            if (skipped < offset) return Bytes.empty();
        }

        byte[] readBuffer = new byte[len > 4096 ? 4096 : len];

        final int bufferLength = readBuffer.length;
        int bytesToRead = bufferLength;
        if (len > 0 && len < bufferLength) {
            bytesToRead = len;
        }

        try (final Bytes.BuilderStream builder = builderFactory.apply(len)) {
            int read;
            long totalRead = 0;
            while (bytesToRead > 0 && -1 != (read = streamToDrain.read(readBuffer, 0, bytesToRead))) {
                builder.write(readBuffer, 0, read);
                totalRead += read;
                if (len > 0) { // only adjust length if not reading to the end
                    // Note the cast must work because buffer.length is an integer
                    bytesToRead = (int) Math.min(len - totalRead, bufferLength);
                }
            }

            return builder.toBytes();
        }
    }

    public static Bytes readFrom(InputStream streamToDrain, int minChunkSize, int maxChunkSize, IntFunction<Bytes.BuilderStream> builderFactory) throws IOException {
        List<Bytes> chunks = new ArrayList<>();

        // copy the inbound bytes into a list of chunks; the chunk size
        // grows exponentially to support both short and long streams.
        int chunkSize = minChunkSize;
        byte[] readBuffer = new byte[chunkSize];
        while (true) {
            if (readBuffer.length != chunkSize) {
                // re-allocate buffer if it does not match the new chunk size
                readBuffer = new byte[chunkSize];
            }
            Bytes chunk = readChunk(streamToDrain, readBuffer, builderFactory);
            if (chunk == null) {
                break;
            }
            chunks.add(chunk);
            chunkSize = Math.min(chunkSize * 2, maxChunkSize);
        }

        if (chunks.size() > 1) {
            return ConcatBytes.wrap(chunks);
        } else if (chunks.isEmpty()) {
            return Bytes.empty();
        } else {
            return chunks.get(0);
        }
    }

    /**
     * Blocks until a chunk of the size give by buffer length can be made from the
     * stream, or EOF is reached. Calls read() repeatedly in case the
     * given stream implementation doesn't completely fill the given
     * buffer in one read() call.
     *
     * @return A chunk of the desired size, or else a chunk as large as
     * was available when end of stream was reached. Returns null if the
     * given stream had no more data in it.
     */
    private static Bytes readChunk(InputStream in, final byte[] buffer, IntFunction<Bytes.BuilderStream> builderFactory) throws IOException {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            final int count = in.read(buffer, bytesRead, buffer.length - bytesRead);
            if (count == -1) {
                break;
            }
            bytesRead += count;
        }

        if (bytesRead == 0) {
            return null;
        } else {
            try (final Bytes.BuilderStream builder = builderFactory.apply(bytesRead)) {
                builder.write(buffer, 0, bytesRead);
                return builder.toBytes();
            }
        }
    }

}
