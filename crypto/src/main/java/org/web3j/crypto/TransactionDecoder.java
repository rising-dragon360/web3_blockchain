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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;

import org.web3j.crypto.transaction.type.TransactionType;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import static java.util.stream.Collectors.toList;

public class TransactionDecoder {
    private static final int UNSIGNED_EIP1559TX_RLP_LIST_SIZE = 9;
    private static final int UNSIGNED_EIP2930TX_RLP_LIST_SIZE = 8;
    private static final int UNSIGNED_EIP4844TX_RLP_LIST_SIZE = 11;

    public static RawTransaction decode(final String hexTransaction) {
        final byte[] transaction = Numeric.hexStringToByteArray(hexTransaction);
        TransactionType transactionType = getTransactionType(transaction);

        switch (transactionType) {
            case EIP1559:
                return decodeEIP1559Transaction(transaction);
            case EIP4844:
                return decodeEIP4844Transaction(transaction);
            case EIP2930:
                return decodeEIP2930Transaction(transaction);
            default:
                return decodeLegacyTransaction(transaction);
        }
    }

    private static TransactionType getTransactionType(final byte[] transaction) {
        // The first byte indicates a transaction type.
        byte firstByte = transaction[0];
        if (firstByte == TransactionType.EIP1559.getRlpType()) return TransactionType.EIP1559;
        else if (firstByte == TransactionType.EIP4844.getRlpType()) return TransactionType.EIP4844;
        else if (firstByte == TransactionType.EIP2930.getRlpType()) return TransactionType.EIP2930;
        else return TransactionType.LEGACY;
    }

    private static RawTransaction decodeEIP4844Transaction(final byte[] transaction) {
        final byte[] encodedTx = Arrays.copyOfRange(transaction, 1, transaction.length);
        final RlpList rlpList = RlpDecoder.decode(encodedTx);
        final RlpList outerList = (RlpList) rlpList.getValues().get(0);

        // Decode the transaction payload
        final RlpList txPayload = (RlpList) outerList.getValues().get(0);
        final List<RlpType> txValues = txPayload.getValues();

        final long chainId = ((RlpString) txValues.get(0)).asPositiveBigInteger().longValue();
        final BigInteger nonce = ((RlpString) txValues.get(1)).asPositiveBigInteger();
        final BigInteger maxPriorityFeePerGas =
                ((RlpString) txValues.get(2)).asPositiveBigInteger();
        final BigInteger maxFeePerGas = ((RlpString) txValues.get(3)).asPositiveBigInteger();
        final BigInteger gasLimit = ((RlpString) txValues.get(4)).asPositiveBigInteger();
        final String to = ((RlpString) txValues.get(5)).asString();
        final BigInteger value = ((RlpString) txValues.get(6)).asPositiveBigInteger();
        final String data = ((RlpString) txValues.get(7)).asString();
        final BigInteger maxFeePerBlobGas = ((RlpString) txValues.get(9)).asPositiveBigInteger();
        final List<Bytes> versionedHashes =
                decodeVersionedHashes(((RlpList) txValues.get(10)).getValues());

        // Decode blobs, commitments, and proofs
        final List<Blob> blobs = decodeBlobs(((RlpList) outerList.getValues().get(1)).getValues());
        final List<Bytes> kzgCommitments =
                decodeBytesList(((RlpList) outerList.getValues().get(2)).getValues());
        final List<Bytes> kzgProofs =
                decodeBytesList(((RlpList) outerList.getValues().get(3)).getValues());

        // Create the raw transaction object
        final RawTransaction rawTransaction =
                RawTransaction.createTransaction(
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

        // Handle signature if present
        if (txValues.size() > UNSIGNED_EIP4844TX_RLP_LIST_SIZE) {
            final byte[] v =
                    Sign.getVFromRecId(
                            Numeric.toBigInt(((RlpString) txValues.get(11)).getBytes()).intValue());
            final byte[] r =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) txValues.get(12)).getBytes()), 32);
            final byte[] s =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) txValues.get(13)).getBytes()), 32);
            final Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            return new SignedRawTransaction(rawTransaction.getTransaction(), signatureData);
        }

        return rawTransaction;
    }

    // Helper methods to decode blobs, commitments, and proofs
    private static List<Blob> decodeBlobs(List<RlpType> rlpBlobs) {
        // Implement decoding logic for blobs
        return rlpBlobs.stream()
                .map(r -> new Blob(((RlpString) r).getBytes()))
                .collect(Collectors.toList());
    }

    private static List<Bytes> decodeBytesList(List<RlpType> rlpBytesList) {
        // Implement decoding logic for commitments and proofs
        return rlpBytesList.stream()
                .map(r -> Bytes.wrap(((RlpString) r).getBytes()))
                .collect(Collectors.toList());
    }

    private static RawTransaction decodeEIP1559Transaction(final byte[] transaction) {
        final byte[] encodedTx = Arrays.copyOfRange(transaction, 1, transaction.length);
        final RlpList rlpList = RlpDecoder.decode(encodedTx);
        final RlpList values = (RlpList) rlpList.getValues().get(0);

        final long chainId =
                ((RlpString) values.getValues().get(0)).asPositiveBigInteger().longValue();
        final BigInteger nonce = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();
        final BigInteger maxPriorityFeePerGas =
                ((RlpString) values.getValues().get(2)).asPositiveBigInteger();
        final BigInteger maxFeePerGas =
                ((RlpString) values.getValues().get(3)).asPositiveBigInteger();
        final BigInteger gasLimit = ((RlpString) values.getValues().get(4)).asPositiveBigInteger();
        final String to = ((RlpString) values.getValues().get(5)).asString();

        final BigInteger value = ((RlpString) values.getValues().get(6)).asPositiveBigInteger();
        final String data = ((RlpString) values.getValues().get(7)).asString();

        final RawTransaction rawTransaction =
                RawTransaction.createTransaction(
                        chainId,
                        nonce,
                        gasLimit,
                        to,
                        value,
                        data,
                        maxPriorityFeePerGas,
                        maxFeePerGas);

        if (values.getValues().size() == UNSIGNED_EIP1559TX_RLP_LIST_SIZE) {
            return rawTransaction;
        } else {
            final byte[] v =
                    Sign.getVFromRecId(
                            Numeric.toBigInt(((RlpString) values.getValues().get(9)).getBytes())
                                    .intValue());
            final byte[] r =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(10)).getBytes()),
                            32);
            final byte[] s =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(11)).getBytes()),
                            32);
            final Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            return new SignedRawTransaction(rawTransaction.getTransaction(), signatureData);
        }
    }

    private static RawTransaction decodeLegacyTransaction(final byte[] transaction) {
        final RlpList rlpList = RlpDecoder.decode(transaction);
        final RlpList values = (RlpList) rlpList.getValues().get(0);
        final BigInteger nonce = ((RlpString) values.getValues().get(0)).asPositiveBigInteger();
        final BigInteger gasPrice = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();
        final BigInteger gasLimit = ((RlpString) values.getValues().get(2)).asPositiveBigInteger();
        final String to = ((RlpString) values.getValues().get(3)).asString();
        final BigInteger value = ((RlpString) values.getValues().get(4)).asPositiveBigInteger();
        final String data = ((RlpString) values.getValues().get(5)).asString();
        if (values.getValues().size() == 6
                || (values.getValues().size() == 8
                        && ((RlpString) values.getValues().get(7)).getBytes().length == 10)
                || (values.getValues().size() == 9
                        && ((RlpString) values.getValues().get(8)).getBytes().length == 10)) {
            // the 8th or 9nth element is the hex
            // representation of "restricted" for private transactions
            return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
        } else {
            final byte[] v = ((RlpString) values.getValues().get(6)).getBytes();
            final byte[] r =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(7)).getBytes()),
                            32);
            final byte[] s =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(8)).getBytes()),
                            32);
            final Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            return new SignedRawTransaction(
                    nonce, gasPrice, gasLimit, to, value, data, signatureData);
        }
    }

    private static RawTransaction decodeEIP2930Transaction(final byte[] transaction) {
        final byte[] encodedTx = Arrays.copyOfRange(transaction, 1, transaction.length);
        final RlpList rlpList = RlpDecoder.decode(encodedTx);
        final RlpList values = (RlpList) rlpList.getValues().get(0);

        final long chainId =
                ((RlpString) values.getValues().get(0)).asPositiveBigInteger().longValue();
        final BigInteger nonce = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();
        final BigInteger gasPrice = ((RlpString) values.getValues().get(2)).asPositiveBigInteger();
        final BigInteger gasLimit = ((RlpString) values.getValues().get(3)).asPositiveBigInteger();
        final String to = ((RlpString) values.getValues().get(4)).asString();
        final BigInteger value = ((RlpString) values.getValues().get(5)).asPositiveBigInteger();
        final String data = ((RlpString) values.getValues().get(6)).asString();
        List<AccessListObject> accessList =
                decodeAccessList(((RlpList) values.getValues().get(7)).getValues());

        final RawTransaction rawTransaction =
                RawTransaction.createTransaction(
                        chainId, nonce, gasPrice, gasLimit, to, value, data, accessList);

        if (values.getValues().size() == UNSIGNED_EIP2930TX_RLP_LIST_SIZE) {
            return rawTransaction;
        } else {
            final byte[] v =
                    Sign.getVFromRecId(
                            Numeric.toBigInt(((RlpString) values.getValues().get(8)).getBytes())
                                    .intValue());
            final byte[] r =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(9)).getBytes()),
                            32);
            final byte[] s =
                    Numeric.toBytesPadded(
                            Numeric.toBigInt(((RlpString) values.getValues().get(10)).getBytes()),
                            32);
            final Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            return new SignedRawTransaction(rawTransaction.getTransaction(), signatureData);
        }
    }

    private static List<AccessListObject> decodeAccessList(List<RlpType> rlp) {
        return rlp.stream()
                .map(rawEntry -> ((RlpList) rawEntry).getValues())
                .map(
                        values -> {
                            return new AccessListObject(
                                    ((RlpString) values.get(0)).asString(),
                                    ((RlpList) values.get(1))
                                            .getValues().stream()
                                                    .map(rawKey -> ((RlpString) rawKey).asString())
                                                    .collect(toList()));
                        })
                .collect(toList());
    }

    public static List<Bytes> decodeVersionedHashes(List<RlpType> rlp) {
        return rlp.stream()
                .map(
                        rlpType -> {
                            // Ensure the RlpType is actually an RlpString
                            if (rlpType instanceof RlpString) {
                                RlpString rlpString = (RlpString) rlpType;
                                // Convert the RlpString to Bytes32
                                return Bytes.wrap(rlpString.getBytes());
                            } else {
                                throw new IllegalArgumentException(
                                        "List contains non-RlpString elements");
                            }
                        })
                .collect(Collectors.toList());
    }
}
