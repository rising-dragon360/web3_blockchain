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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.bytes.Bytes;

public class BlobUtils {

    static {
        CKZG4844JNI.loadNativeLibrary();
    }

    private static final byte blobCommitmentVersionKZG = 0x01;

    public static void loadTrustedSetupFromResource() {
        try (InputStream resourceStream =
                BlobUtils.class.getClassLoader().getResourceAsStream("trusted_setup.txt")) {
            if (resourceStream == null) {
                throw new IllegalArgumentException("Resource not found");
            }
            Path tempFile = Files.createTempFile("trusted_setup", "txt");
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            CKZG4844JNI.loadTrustedSetup(tempFile.toString());
            tempFile.toFile().deleteOnExit(); // Delete temp file when JVM exits
        } catch (Exception e) {
            throw new RuntimeException("Failed to load trusted setup from resource", e);
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
