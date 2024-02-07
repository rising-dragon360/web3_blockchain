/*
 * Copyright 2024 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.bytes.Bytes;

public class BlobUtils {

    private static final byte blobCommitmentVersionKZG = 0x01;
    private static final String trustedSetupFilePath = "trusted_setup.txt";

    static {
        CKZG4844JNI.loadNativeLibrary();
        loadTrustedSetupParameters(trustedSetupFilePath);
    }

    public static void loadTrustedSetupParameters(String filePath) {
        try (InputStream inputStream =
                BlobUtils.class.getClassLoader().getResourceAsStream(filePath)) {
            assert inputStream != null;
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            final int g1Count = Integer.parseInt(reader.readLine());
            final int g2Count = Integer.parseInt(reader.readLine());

            final ByteBuffer g1 = ByteBuffer.allocate(g1Count * CKZG4844JNI.BYTES_PER_G1);
            final ByteBuffer g2 = ByteBuffer.allocate(g2Count * CKZG4844JNI.BYTES_PER_G2);

            for (int i = 0; i < g1Count; i++) {
                g1.put(Bytes.fromHexString(reader.readLine()).toArray());
            }
            for (int i = 0; i < g2Count; i++) {
                g2.put(Bytes.fromHexString(reader.readLine()).toArray());
            }
            CKZG4844JNI.loadTrustedSetup(g1.array(), g1Count, g2.array(), g2Count);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Bytes getCommitment(Blob blobData) {
        return Bytes.wrap(CKZG4844JNI.blobToKzgCommitment(blobData.data.toArray()));
    }

    public static Bytes getProof(Blob blobData, Bytes commitment) {
        return Bytes.wrap(
                CKZG4844JNI.computeBlobKzgProof(blobData.data.toArray(), commitment.toArray()));
    }

    public static boolean checkProofValidity(Blob blobData, Bytes commitment, Bytes proof) {
        return CKZG4844JNI.verifyBlobKzgProof(
                blobData.data.toArray(), commitment.toArray(), proof.toArray());
    }

    public static Bytes kzgToVersionedHash(Bytes commitment) {
        byte[] hash = Hash.sha256(commitment.toArray());
        hash[0] = blobCommitmentVersionKZG;
        return Bytes.wrap(hash);
    }

    public static void freeTrustedSetup() {
        // the current trusted setup should be freed before a new one is loaded
        CKZG4844JNI.freeTrustedSetup();
    }
}
