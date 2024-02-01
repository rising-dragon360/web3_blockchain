/*
 * Copyright 2019 Web3 Labs Ltd.
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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import ethereum.ckzg4844.CKZG4844JNI;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;

import org.web3j.crypto.transaction.type.Transaction1559;
import org.web3j.crypto.transaction.type.Transaction2930;
import org.web3j.crypto.transaction.type.Transaction4844;
import org.web3j.utils.Numeric;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransactionDecoderTest {

    private static final int FIELD_ELEMENT_SIZE = 32; // Size of each field element in bytes
    private static final int BLOB_SIZE = 131072; // Total size of the blob in bytes
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    public void testDecoding() throws Exception {
        BigInteger nonce = BigInteger.ZERO;
        BigInteger gasPrice = BigInteger.ONE;
        BigInteger gasLimit = BigInteger.TEN;
        String to = "0x0add5355";
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE);
        RawTransaction rawTransaction =
                RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value);
        byte[] encodedMessage = TransactionEncoder.encode(rawTransaction);
        String hexMessage = Numeric.toHexString(encodedMessage);

        RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertNotNull(result);
        assertEquals(nonce, result.getNonce());
        assertEquals(gasPrice, result.getGasPrice());
        assertEquals(gasLimit, result.getGasLimit());
        assertEquals(to, result.getTo());
        assertEquals(value, result.getValue());
        assertEquals("", result.getData());
    }

    @Test
    public void testDecodingSigned() throws Exception {
        BigInteger nonce = BigInteger.ZERO;
        BigInteger gasPrice = BigInteger.ONE;
        BigInteger gasLimit = BigInteger.TEN;
        String to = "0x0add5355";
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE);
        RawTransaction rawTransaction =
                RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value);
        byte[] signedMessage =
                TransactionEncoder.signMessage(rawTransaction, SampleKeys.CREDENTIALS);
        String hexMessage = Numeric.toHexString(signedMessage);

        RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertNotNull(result);
        assertEquals(nonce, result.getNonce());
        assertEquals(gasPrice, result.getGasPrice());
        assertEquals(gasLimit, result.getGasLimit());
        assertEquals(to, result.getTo());
        assertEquals(value, result.getValue());
        assertEquals("", result.getData());
        assertTrue(result instanceof SignedRawTransaction);
        SignedRawTransaction signedResult = (SignedRawTransaction) result;
        assertNotNull(signedResult.getSignatureData());
        Sign.SignatureData signatureData = signedResult.getSignatureData();
        byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
        BigInteger key = Sign.signedMessageToKey(encodedTransaction, signatureData);
        assertEquals(key, SampleKeys.PUBLIC_KEY);
        assertEquals(SampleKeys.ADDRESS, signedResult.getFrom());
        signedResult.verify(SampleKeys.ADDRESS);
        assertNull(signedResult.getChainId());
    }

    @Test
    public void testDecodingSignedChainId() throws Exception {
        BigInteger nonce = BigInteger.ZERO;
        BigInteger gasPrice = BigInteger.ONE;
        BigInteger gasLimit = BigInteger.TEN;
        String to = "0x0add5355";
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE);
        long chainId = 46;
        RawTransaction rawTransaction =
                RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, to, value);
        byte[] signedMessage =
                TransactionEncoder.signMessage(rawTransaction, chainId, SampleKeys.CREDENTIALS);
        String hexMessage = Numeric.toHexString(signedMessage);

        RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertNotNull(result);
        assertEquals(nonce, result.getNonce());
        assertEquals(gasPrice, result.getGasPrice());
        assertEquals(gasLimit, result.getGasLimit());
        assertEquals(to, result.getTo());
        assertEquals(value, result.getValue());
        assertEquals("", result.getData());
        assertTrue(result instanceof SignedRawTransaction);
        SignedRawTransaction signedResult = (SignedRawTransaction) result;
        assertEquals(SampleKeys.ADDRESS, signedResult.getFrom());
        signedResult.verify(SampleKeys.ADDRESS);
        assertEquals(chainId, signedResult.getChainId().longValue());
    }

    @Test
    public void testRSize31() throws Exception {

        String hexTransaction =
                "0xf883370183419ce09433c98f20dd73d7bb1d533c4aa3371f2b30c6ebde80a45093dc7d00000000000000000000000000000000000000000000000000000000000000351c9fb90996c836fb34b782ee3d6efa9e2c79a75b277c014e353b51b23b00524d2da07435ebebca627a51a863bf590aff911c4746ab8386a0477c8221bb89671a5d58";

        RawTransaction result = TransactionDecoder.decode(hexTransaction);
        SignedRawTransaction signedResult = (SignedRawTransaction) result;
        assertEquals("0x1b609b03e2e9b0275a61fa5c69a8f32550285536", signedResult.getFrom());
    }

    @Test
    public void testDecoding1559() {
        final RawTransaction rawTransaction = createEip1559RawTransaction();
        final Transaction1559 transaction1559 = (Transaction1559) rawTransaction.getTransaction();

        final byte[] encodedMessage = TransactionEncoder.encode(rawTransaction);
        final String hexMessage = Numeric.toHexString(encodedMessage);

        final RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertTrue(result.getTransaction() instanceof Transaction1559);
        final Transaction1559 resultTransaction1559 = (Transaction1559) result.getTransaction();

        assertNotNull(result);
        assertEquals(transaction1559.getChainId(), resultTransaction1559.getChainId());
        assertEquals(transaction1559.getNonce(), resultTransaction1559.getNonce());
        assertEquals(transaction1559.getMaxFeePerGas(), resultTransaction1559.getMaxFeePerGas());
        assertEquals(
                transaction1559.getMaxPriorityFeePerGas(),
                resultTransaction1559.getMaxPriorityFeePerGas());
        assertEquals(transaction1559.getGasLimit(), resultTransaction1559.getGasLimit());
        assertEquals(transaction1559.getTo(), resultTransaction1559.getTo());
        assertEquals(transaction1559.getValue(), resultTransaction1559.getValue());
        assertEquals(transaction1559.getData(), resultTransaction1559.getData());
    }

    @Test
    public void testDecodingSigned1559() throws SignatureException {
        final RawTransaction rawTransaction = createEip1559RawTransaction();
        final Transaction1559 transaction1559 = (Transaction1559) rawTransaction.getTransaction();

        final byte[] signedMessage =
                TransactionEncoder.signMessage(rawTransaction, SampleKeys.CREDENTIALS);
        final String signedHexMessage = Numeric.toHexString(signedMessage);

        final RawTransaction result = TransactionDecoder.decode(signedHexMessage);
        assertTrue(result.getTransaction() instanceof Transaction1559);
        final Transaction1559 resultTransaction1559 = (Transaction1559) result.getTransaction();

        assertNotNull(result);
        assertEquals(transaction1559.getChainId(), resultTransaction1559.getChainId());
        assertEquals(transaction1559.getNonce(), resultTransaction1559.getNonce());
        assertEquals(transaction1559.getMaxFeePerGas(), resultTransaction1559.getMaxFeePerGas());
        assertEquals(
                transaction1559.getMaxPriorityFeePerGas(),
                resultTransaction1559.getMaxPriorityFeePerGas());
        assertEquals(transaction1559.getGasLimit(), resultTransaction1559.getGasLimit());
        assertEquals(transaction1559.getTo(), resultTransaction1559.getTo());
        assertEquals(transaction1559.getValue(), resultTransaction1559.getValue());
        assertEquals(transaction1559.getData(), resultTransaction1559.getData());

        assertTrue(result instanceof SignedRawTransaction);
        final SignedRawTransaction signedResult = (SignedRawTransaction) result;
        assertNotNull(signedResult.getSignatureData());

        final Sign.SignatureData signatureData = signedResult.getSignatureData();
        final byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
        final BigInteger key = Sign.signedMessageToKey(encodedTransaction, signatureData);
        assertEquals(key, SampleKeys.PUBLIC_KEY);
        assertEquals(SampleKeys.ADDRESS, signedResult.getFrom());
        signedResult.verify(SampleKeys.ADDRESS);
        assertNull(signedResult.getChainId());
    }

    private static RawTransaction createEip1559RawTransaction() {
        return RawTransaction.createEtherTransaction(
                3L,
                BigInteger.valueOf(0),
                BigInteger.valueOf(30000),
                "0x627306090abab3a6e1400e9345bc60c78a8bef57",
                BigInteger.valueOf(123),
                BigInteger.valueOf(5678),
                BigInteger.valueOf(1100000));
    }

    @Test
    public void testDecoding2930() {
        final RawTransaction rawTransaction = createEip2930RawTransaction();
        final Transaction2930 transaction2930 = (Transaction2930) rawTransaction.getTransaction();

        final byte[] encodedMessage = TransactionEncoder.encode(rawTransaction);
        final String hexMessage = Numeric.toHexString(encodedMessage);

        final RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertTrue(result.getTransaction() instanceof Transaction2930);
        final Transaction2930 resultTransaction2930 = (Transaction2930) result.getTransaction();

        assertNotNull(result);
        assertEquals(transaction2930.getChainId(), resultTransaction2930.getChainId());
        assertEquals(transaction2930.getNonce(), resultTransaction2930.getNonce());
        assertEquals(transaction2930.getGasPrice(), resultTransaction2930.getGasPrice());
        assertEquals(transaction2930.getGasLimit(), resultTransaction2930.getGasLimit());
        assertEquals(transaction2930.getTo(), resultTransaction2930.getTo());
        assertEquals(transaction2930.getValue(), resultTransaction2930.getValue());
        assertEquals(transaction2930.getData(), resultTransaction2930.getData());
        assertIterableEquals(
                transaction2930.getAccessList(), resultTransaction2930.getAccessList());
    }

    @Test
    public void testDecodingSigned2930() throws SignatureException {
        final RawTransaction rawTransaction = createEip2930RawTransaction();
        final Transaction2930 transaction2930 = (Transaction2930) rawTransaction.getTransaction();

        final byte[] signedMessage =
                TransactionEncoder.signMessage(rawTransaction, SampleKeys.CREDENTIALS);
        final String signedHexMessage = Numeric.toHexString(signedMessage);

        final RawTransaction result = TransactionDecoder.decode(signedHexMessage);
        assertTrue(result.getTransaction() instanceof Transaction2930);
        final Transaction2930 resultTransaction2930 = (Transaction2930) result.getTransaction();

        assertNotNull(result);
        assertEquals(transaction2930.getChainId(), resultTransaction2930.getChainId());
        assertEquals(transaction2930.getNonce(), resultTransaction2930.getNonce());
        assertEquals(transaction2930.getGasPrice(), resultTransaction2930.getGasPrice());
        assertEquals(transaction2930.getGasLimit(), resultTransaction2930.getGasLimit());
        assertEquals(transaction2930.getTo(), resultTransaction2930.getTo());
        assertEquals(transaction2930.getValue(), resultTransaction2930.getValue());
        assertEquals(transaction2930.getData(), resultTransaction2930.getData());
        assertIterableEquals(
                transaction2930.getAccessList(), resultTransaction2930.getAccessList());

        assertTrue(result instanceof SignedRawTransaction);
        final SignedRawTransaction signedResult = (SignedRawTransaction) result;
        assertNotNull(signedResult.getSignatureData());

        final Sign.SignatureData signatureData = signedResult.getSignatureData();
        final byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
        final BigInteger key = Sign.signedMessageToKey(encodedTransaction, signatureData);
        assertEquals(key, SampleKeys.PUBLIC_KEY);
        assertEquals(SampleKeys.ADDRESS, signedResult.getFrom());
        signedResult.verify(SampleKeys.ADDRESS);
        assertNull(signedResult.getChainId());
    }

    private static RawTransaction createEip2930RawTransaction() {
        // Test example from https://eips.ethereum.org/EIPS/eip-2930
        List<AccessListObject> accessList =
                Stream.of(
                                new AccessListObject(
                                        "0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae",
                                        Stream.of(
                                                        "0x0000000000000000000000000000000000000000000000000000000000000003",
                                                        "0x0000000000000000000000000000000000000000000000000000000000000007")
                                                .collect(toList())),
                                new AccessListObject(
                                        "0xbb9bc244d798123fde783fcc1c72d3bb8c189413",
                                        Collections.emptyList()))
                        .collect(toList());

        return RawTransaction.createTransaction(
                3L,
                BigInteger.valueOf(0),
                BigInteger.valueOf(30000),
                BigInteger.valueOf(500000),
                "0x627306090abab3a6e1400e9345bc60c78a8bef57",
                BigInteger.valueOf(1000000),
                "0x1000001111100000",
                accessList);
    }

    @Test
    public void testDecoding4844() {
        final RawTransaction rawTransaction = createEip4844RawTransaction();
        final Transaction4844 transaction4844 = (Transaction4844) rawTransaction.getTransaction();

        final byte[] encodedMessage = TransactionEncoder.encode(rawTransaction);
        final String hexMessage = Numeric.toHexString(encodedMessage);

        final RawTransaction result = TransactionDecoder.decode(hexMessage);
        assertTrue(result.getTransaction() instanceof Transaction4844);
        final Transaction4844 resultTransaction4844 = (Transaction4844) result.getTransaction();

        assertNotNull(result);
        assertTrue(
                range(0, transaction4844.getBlobs().get().size())
                        .allMatch(
                                i ->
                                        transaction4844
                                                .getBlobs()
                                                .get()
                                                .get(i)
                                                .getData()
                                                .equals(
                                                        resultTransaction4844
                                                                .getBlobs()
                                                                .get()
                                                                .get(i)
                                                                .getData())));
        assertEquals(
                transaction4844.getBlobs().get().get(0).getData(),
                resultTransaction4844.getBlobs().get().get(0).getData());
        assertEquals(transaction4844.getChainId(), resultTransaction4844.getChainId());
        assertEquals(transaction4844.getNonce(), resultTransaction4844.getNonce());
        assertEquals(transaction4844.getMaxFeePerGas(), resultTransaction4844.getMaxFeePerGas());
        assertEquals(
                transaction4844.getMaxPriorityFeePerGas(),
                resultTransaction4844.getMaxPriorityFeePerGas());
        assertEquals(transaction4844.getGasLimit(), resultTransaction4844.getGasLimit());
        assertEquals(transaction4844.getTo(), resultTransaction4844.getTo());
        assertEquals(transaction4844.getValue(), resultTransaction4844.getValue());
        assertEquals(transaction4844.getData(), resultTransaction4844.getData());
    }

    @Test
    public void testDecodingSigned4844() {
        final RawTransaction rawTransaction = createEip4844RawTransaction();
        final Transaction4844 transaction4844 = (Transaction4844) rawTransaction.getTransaction();

        final byte[] signedMessage =
                TransactionEncoder.signMessage(rawTransaction, SampleKeys.CREDENTIALS);
        final String signedHexMessage = Numeric.toHexString(signedMessage);

        final RawTransaction result = TransactionDecoder.decode(signedHexMessage);
        assertTrue(result.getTransaction() instanceof Transaction4844);
        final Transaction4844 resultTransaction4844 = (Transaction4844) result.getTransaction();

        assertNotNull(result);
        assertTrue(
                range(0, transaction4844.getBlobs().get().size())
                        .allMatch(
                                i ->
                                        transaction4844
                                                .getBlobs()
                                                .get()
                                                .get(i)
                                                .getData()
                                                .equals(
                                                        resultTransaction4844
                                                                .getBlobs()
                                                                .get()
                                                                .get(i)
                                                                .getData())));
        assertEquals(transaction4844.getChainId(), resultTransaction4844.getChainId());
        assertEquals(transaction4844.getNonce(), resultTransaction4844.getNonce());
        assertEquals(transaction4844.getMaxFeePerGas(), resultTransaction4844.getMaxFeePerGas());
        assertEquals(
                transaction4844.getMaxPriorityFeePerGas(),
                resultTransaction4844.getMaxPriorityFeePerGas());
        assertEquals(transaction4844.getGasLimit(), resultTransaction4844.getGasLimit());
        assertEquals(transaction4844.getTo(), resultTransaction4844.getTo());
        assertEquals(transaction4844.getValue(), resultTransaction4844.getValue());
        assertEquals(transaction4844.getData(), resultTransaction4844.getData());
    }

    private static RawTransaction createEip4844RawTransaction() {
        List<Blob> blobs = new ArrayList<>();

        blobs.add(new Blob(createRandomBlob()));
        blobs.add(new Blob(createRandomBlob()));
        return RawTransaction.createTransaction(
                blobs,
                1559L,
                BigInteger.valueOf(0),
                BigInteger.valueOf(5678),
                BigInteger.valueOf(1100000),
                BigInteger.valueOf(30000),
                "0x627306090abab3a6e1400e9345bc60c78a8bef57",
                BigInteger.valueOf(0),
                "",
                BigInteger.valueOf(30000));
    }

    //    @Test
    //    public void testBlobToCommitmentProofVersionedHashes() {
    //        BlobUtils.loadTrustedSetupFromResource();
    //        Blob blob = new Blob(createRandomBlob());
    //        byte[] commitment = CKZG4844JNI.blobToKzgCommitment(blob.getData().toArray());
    //        byte[] proofs = CKZG4844JNI.computeBlobKzgProof(blob.getData().toArray(), commitment);
    //        System.out.println("commitments = " + Arrays.toString(commitment));
    //        System.out.println("proofs = " + Arrays.toString(proofs));
    //        BlobUtils.freeTrustedSetup();
    //    }
    //
    //    @Test
    //    public void testBlobUtils() {
    //        BlobUtils.loadTrustedSetupFromResource();
    //        Blob blob = new Blob(createRandomBlob());
    //        System.out.println("blob = " + blob.getData());
    //        BlobUtils blobUtils = new BlobUtils(blob);
    //        System.out.println("commitments = " + blobUtils.getCommitment());
    //        System.out.println("proofs = " + blobUtils.getProof(blobUtils.getCommitment()));
    //        System.out.println(
    //                "versioned Hashes = " +
    // blobUtils.kzgToVersionedHash(blobUtils.getCommitment()));
    //        BlobUtils.freeTrustedSetup();
    //    }

    private static UInt256 randomBLSFieldElement() {
        final BigInteger attempt = new BigInteger(CKZG4844JNI.BLS_MODULUS.bitLength(), RANDOM);
        if (attempt.compareTo(CKZG4844JNI.BLS_MODULUS) < 0) {
            return UInt256.valueOf(attempt);
        }
        return randomBLSFieldElement();
    }

    public static byte[] flatten(final byte[]... bytes) {
        final int capacity = Arrays.stream(bytes).mapToInt(b -> b.length).sum();
        final ByteBuffer buffer = ByteBuffer.allocate(capacity);
        Arrays.stream(bytes).forEach(buffer::put);
        return buffer.array();
    }

    public static byte[] createRandomBlob() {
        final byte[][] blob =
                range(0, CKZG4844JNI.FIELD_ELEMENTS_PER_BLOB)
                        .mapToObj(__ -> randomBLSFieldElement())
                        .map(fieldElement -> fieldElement.toArray(ByteOrder.BIG_ENDIAN))
                        .toArray(byte[][]::new);
        return flatten(blob);
    }
}
