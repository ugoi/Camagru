package com.camagru;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;

/**
 * A class that manages the creation and verification of jwt tokens
 */
public class JwtManager {
    private String algorithm = "HmacSHA256";
    private Mac mac;

    /**
     * Creates a new JwtManager with the given secret
     *
     * @param secret
     *               A string which is the secret key used to sign the jwt token.
     * @throws NoSuchAlgorithmException
     *                                  If the algorithm is not available
     * @throws InvalidKeyException
     *                                  If the key is invalid
     */
    public JwtManager(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        mac = Mac.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), algorithm);
        mac.init(secretKeySpec);
    }

    /**
     * Creates a jwt token with the given sub
     *
     * @param sub
     *            A string which is the subject of the token. This is the user id.
     * @return A string which is the jwt token.
     */
    public String createToken(String sub) {
        String header = new JSONObject()
                .put("alg", "HS256")
                .put("typ", "JWT")
                .toString();

        String payload = new JSONObject()
                .put("iss", "camguru")
                .put("exp", Instant.now().plus(Duration.ofDays(30)).getEpochSecond())
                .put("sub", sub)
                .toString();

        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        String signature = sign(encodedHeader + "." + encodedPayload);

        String jwt = encodedHeader + "." + encodedPayload + "." + signature;
        return jwt;
    }

    /**
     * Decodes the given jwt token
     *
     * @param token
     *              A string which is the jwt token.
     * @return A JSONObject which contains the header and payload of the token.
     */
    public JSONObject decodeToken(String token) {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        JSONObject header = new JSONObject(new String(decoder.decode(chunks[0])));
        JSONObject payload = new JSONObject(new String(decoder.decode(chunks[1])));
        return new JSONObject()
                .put("header", header)
                .put("payload", payload);
    }

    /**
     * Verifies the signature of the given jwt token
     *
     * @param token
     *              A string which is the jwt token.
     * 
     * @throws IllegalArgumentException
     *                                  If the signature is not verified.
     */
    public void verifySignature(String token) {
        String[] chunks = token.split("\\.");
        String encodedHeader = chunks[0];
        String encodedPayload = chunks[1];
        String encodedSignature = chunks[2];
        String signature = sign(encodedHeader + "." + encodedPayload);

        // Throw exception if signature is not verified
        if (!signature.equals(encodedSignature)) {
            throw new IllegalArgumentException("Invalid signature");
        }
    }

    /**
     * Signs the given data
     *
     * @param data
     *             A string which is the data to be signed.
     * @return A string which is the signature.
     */
    private String sign(String data) {
        byte[] rawSignature = (mac.doFinal((data).getBytes(StandardCharsets.UTF_8)));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature);
        return signature;
    }

}
