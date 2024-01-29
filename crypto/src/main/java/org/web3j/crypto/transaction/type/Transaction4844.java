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
package org.web3j.crypto.transaction.type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.web3j.abi.datatypes.Bytes;
import org.web3j.crypto.Blob;
import org.web3j.crypto.BlobUtils;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

public class Transaction4844 extends Transaction1559 implements ITransaction {

    private final BigInteger maxFeePerBlobGas;
    private final List<Bytes> versionedHashes;
    private final Optional<List<Blob>> blobs;
    private final Optional<List<Bytes>> kzgProofs;
    private final Optional<List<Bytes>> kzgCommitments;

    public Transaction4844(
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas,
            List<Bytes> versionedHashes) {
        super(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
        this.maxFeePerBlobGas = maxFeePerBlobGas;
        this.versionedHashes = versionedHashes;
        this.blobs = Optional.empty();
        this.kzgCommitments = Optional.empty();
        this.kzgProofs = Optional.empty();
    }

    public Transaction4844(
            List<Blob> blobs,
            List<Bytes> kzgCommitments,
            List<Bytes> kzgProofs,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas,
            List<Bytes> versionedHashes) {
        super(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
        this.maxFeePerBlobGas = maxFeePerBlobGas;
        this.versionedHashes = versionedHashes;
        this.blobs = Optional.ofNullable(blobs);
        this.kzgCommitments = Optional.ofNullable(kzgCommitments);
        this.kzgProofs = Optional.ofNullable(kzgProofs);
    }

    public Transaction4844(
            List<Blob> blobsData,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas) {
        super(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
        this.maxFeePerBlobGas = maxFeePerBlobGas;
        this.blobs = Optional.ofNullable(blobsData);
        if (!BlobUtils.libraryLoaded) {
            BlobUtils.loadTrustedSetupFromResource();
        }
        List<BlobUtils> blobUtils =
                blobsData.stream().map(BlobUtils::new).collect(Collectors.toList());
        this.kzgCommitments =
                Optional.of(
                        blobUtils.stream()
                                .map(BlobUtils::getCommitment)
                                .collect(Collectors.toList()));
        this.kzgProofs =
                Optional.of(
                        IntStream.range(0, blobUtils.size())
                                .mapToObj(
                                        i -> blobUtils.get(i).getProof(kzgCommitments.get().get(i)))
                                .collect(Collectors.toList()));
        this.versionedHashes =
                IntStream.range(0, blobUtils.size())
                        .mapToObj(
                                i ->
                                        blobUtils
                                                .get(i)
                                                .kzgToVersionedHash(kzgCommitments.get().get(i)))
                        .collect(Collectors.toList());
    }

    @Override
    public List<RlpType> asRlpValues(Sign.SignatureData signatureData) {
        List<RlpType> resultTx = new ArrayList<>();

        resultTx.add(RlpString.create(getChainId()));
        resultTx.add(RlpString.create(getNonce()));
        resultTx.add(RlpString.create(getMaxPriorityFeePerGas()));
        resultTx.add(RlpString.create(getMaxFeePerGas()));
        resultTx.add(RlpString.create(getGasLimit()));

        String to = getTo();
        if (to != null && to.length() > 0) {
            resultTx.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            resultTx.add(RlpString.create(""));
        }

        resultTx.add(RlpString.create(getValue()));
        byte[] data = Numeric.hexStringToByteArray(getData());
        resultTx.add(RlpString.create(data));

        // access list
        resultTx.add(new RlpList());

        // Blob Transaction: max_fee_per_blob_gas and versioned_hashes
        resultTx.add(RlpString.create(getMaxFeePerBlobGas()));
        resultTx.add(new RlpList(getVersionedHashes()));

        if (signatureData != null) {
            resultTx.add(RlpString.create(Sign.getRecId(signatureData, getChainId())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getR())));
            resultTx.add(
                    RlpString.create(
                            org.web3j.utils.Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        List<RlpType> result = new ArrayList<>();
        result.add(new RlpList(resultTx));
        // Adding blobs, commitments, and proofs
        result.add(new RlpList(getBlobs()));
        result.add(new RlpList(getKzgCommitments()));
        result.add(new RlpList(getKzgProofs()));

        return result;
    }

    public static Transaction4844 createTransaction(
            List<Blob> blobs,
            List<Bytes> kzgCommitments,
            List<Bytes> kzgProofs,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas,
            List<Bytes> versionedHashes) {

        return new Transaction4844(
                blobs,
                kzgCommitments,
                kzgProofs,
                chainId,
                nonce,
                maxPriorityFeePerGas,
                maxFeePerGas,
                gasLimit,
                to,
                value,
                data,
                maxFeePerBlobGas,
                versionedHashes);
    }

    public static Transaction4844 createTransaction(
            List<Blob> blobs,
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas) {

        return new Transaction4844(
                blobs,
                chainId,
                nonce,
                maxPriorityFeePerGas,
                maxFeePerGas,
                gasLimit,
                to,
                value,
                data,
                maxFeePerBlobGas);
    }

    public static Transaction4844 createTransaction(
            long chainId,
            BigInteger nonce,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxFeePerBlobGas,
            List<Bytes> versionedHashes) {

        return new Transaction4844(
                chainId,
                nonce,
                maxPriorityFeePerGas,
                maxFeePerGas,
                gasLimit,
                to,
                value,
                data,
                maxFeePerBlobGas,
                versionedHashes);
    }

    public BigInteger getMaxFeePerBlobGas() {
        return maxFeePerBlobGas;
    }

    public List<RlpType> getVersionedHashes() {
        return versionedHashes.stream()
                .map(hash -> RlpString.create(hash.getValue()))
                .collect(Collectors.toList());
    }

    public List<RlpType> getKzgCommitments() {
        return kzgCommitments
                .<List<RlpType>>map(
                        bytesList ->
                                bytesList.stream()
                                        .map(bytes -> RlpString.create(bytes.getValue()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<RlpType> getKzgProofs() {
        return kzgProofs
                .<List<RlpType>>map(
                        bytesList ->
                                bytesList.stream()
                                        .map(bytes -> RlpString.create(bytes.getValue()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<RlpType> getBlobs() {
        return blobs.<List<RlpType>>map(
                        blobList ->
                                blobList.stream()
                                        .map(blob -> RlpString.create(blob.getData().getValue()))
                                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public TransactionType getType() {
        return TransactionType.EIP4844;
    }
}
