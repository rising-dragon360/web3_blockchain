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

import org.web3j.crypto.transaction.type.ITransaction;

public class SignedRawTransaction extends RawTransaction implements SignatureDataOperations {

    private final Sign.SignatureData signatureData;

    public SignedRawTransaction(
            BigInteger nonce,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            Sign.SignatureData signatureData) {
        super(nonce, gasPrice, gasLimit, to, value, data);
        this.signatureData = signatureData;
    }

    public SignedRawTransaction(ITransaction transaction, Sign.SignatureData signatureData) {
        super(transaction);
        this.signatureData = signatureData;
    }

    public Sign.SignatureData getSignatureData() {
        return signatureData;
    }

    @Override
    public byte[] getEncodedTransaction(Long chainId) {
        if (this.getTransaction().getType().isEip4844()) {
            return TransactionEncoder.encode4844(this);
        } else if (null == chainId) {
            return TransactionEncoder.encode(this);
        } else {
            return TransactionEncoder.encode(this, chainId);
        }
    }
}
