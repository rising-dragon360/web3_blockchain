/*
 * Copyright 2023 Web3 Labs Ltd.
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
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECPoint;
import java.util.Base64;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.web3j.crypto.Secp256k1JWK.isEcCoordBase64Valid;

public class Secp256k1JWKTest {

    private static final String CRV = "secp256k1";
    private static final String KTY = "EC";
    private static final String ECDSA = "ECDSA";

    @Test
    public void testPublicJWKCreation()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                    NoSuchProviderException {
        KeyPair keyPair = Keys.createSecp256k1KeyPair();
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        Secp256k1JWK jwk = new Secp256k1JWK.Builder(publicKey).build();
        assertPublicJWK(jwk);
        assertNull(jwk.getD());
    }

    @Test
    public void testPrivateJWKCreation()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
                    NoSuchProviderException {
        KeyPair keyPair = Keys.createSecp256k1KeyPair();
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        Secp256k1JWK jwk = new Secp256k1JWK.Builder(publicKey).setPrivateKey(privateKey).build();
        assertTrue(isEcCoordBase64Valid(jwk.getD()));
    }

    private void assertPublicJWK(Secp256k1JWK jwk) {
        assertEquals(CRV, jwk.getCrv());
        assertEquals(KTY, jwk.getKty());
        assertTrue(isEcCoordBase64Valid(jwk.getX()));
        assertTrue(isEcCoordBase64Valid(jwk.getY()));
    }

    @Test
    public void testIsEcCoordBase64Valid() {
        BigInteger example =
                new BigInteger(
                        "3229926951468396372745881109827389213802338353528900010374647556550771243437");
        String message = Base64.getEncoder().encodeToString(example.toByteArray());

        // Format to standard
        message =
                message.replaceAll("[+]", "-")
                        .replaceAll("[/]", "_")
                        .substring(0, message.length() - 1);

        assertTrue(isEcCoordBase64Valid(message));
        assertFalse(isEcCoordBase64Valid(example.toString()));
    }

    @Test
    public void testJWK() {
        // values used to generate key expected pair
        BigInteger x =
                new BigInteger(
                        "3103614958234839665247501408257843475845035043213879914678638594870084849164");
        BigInteger y =
                new BigInteger(
                        "34238765474428234659004539858164663420737427710864517699249283476143567097942");
        BigInteger d =
                new BigInteger(
                        "99708378788594189461095872125253518622900301859996849590285333233583690406956");

        ECPoint ecPoint = new ECPoint(x, y);
        BigInteger n =
                new BigInteger(
                        "115792089237316195423570985008687907852837564279074904382605163141518161494337");

        ECCurve curve = new SecP256K1Curve();

        SecP256K1Point secP256K1Point = (SecP256K1Point) EC5Util.convertPoint(curve, ecPoint);
        ECDomainParameters ecDomainParameters = new ECDomainParameters(curve, secP256K1Point, n);

        ECPublicKeyParameters ecPublicKeyParameters =
                new ECPublicKeyParameters(secP256K1Point, ecDomainParameters);
        BCECPublicKey publicKey = new BCECPublicKey(ECDSA, ecPublicKeyParameters, null);

        ECPrivateKeyParameters privateKeyParameters =
                new ECPrivateKeyParameters(d, ecDomainParameters);
        BCECPrivateKey privateKey = new BCECPrivateKey(ECDSA, privateKeyParameters, null);

        Secp256k1JWK generatedJwk =
                new Secp256k1JWK.Builder(publicKey).setPrivateKey(privateKey).build();
    }
}
