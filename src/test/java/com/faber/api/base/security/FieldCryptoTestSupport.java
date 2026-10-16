package com.faber.api.base.security;

import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.core.crypto.RequestCrypto;
import org.springframework.mock.web.MockHttpServletRequest;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class FieldCryptoTestSupport {
    public static String encrypt(RequestCryptoBiz biz, RequestCryptoBiz.Preparation ready, String path, String field, String value) throws Exception {
        var rsaKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(biz.publicKey().publicKey())));
        var generator = KeyGenerator.getInstance("AES"); generator.init(256);
        var key = generator.generateKey();
        byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
        var aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        aes.updateAAD(RequestCrypto.fieldAad(ready.kid(), ready.challengeId(), ready.purpose(), "POST", path, field));
        var rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
        rsa.init(Cipher.ENCRYPT_MODE, rsaKey, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        return Base64.getEncoder().encodeToString(rsa.doFinal(key.getEncoded())) + "." + Base64.getEncoder().encodeToString(iv)
                + "." + Base64.getEncoder().encodeToString(aes.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static MockHttpServletRequest request(RequestCryptoBiz.Preparation ready, String path) {
        var request = new MockHttpServletRequest("POST", path);
        request.addHeader("FaCryptoVersion", "2"); request.addHeader("FaCryptoKid", ready.kid());
        if (ready.challengeId() != null) request.addHeader("FaCryptoChallengeId", ready.challengeId());
        return request;
    }
}
