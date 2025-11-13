package utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.KeyFactory;
import java.io.FileInputStream;
import java.util.Date;

public class JWTUtils {

    // Đọc private key từ file
    public static RSAPrivateKey readPrivateKey(String filepath) throws Exception {
        FileInputStream fis = new FileInputStream(filepath);
        byte[] keyBytes = new byte[fis.available()];
        fis.read(keyBytes);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));
    }

    // Tạo JWT token
    public static String createJWT(String clientId, String userId, String audience, String privateKeyPath) throws Exception {
        // Đọc private key từ file
        RSAPrivateKey privateKey = readPrivateKey(privateKeyPath);

        // Thời gian hết hạn của JWT token (5 phút)
        long expTime = System.currentTimeMillis() + (5 * 60 * 1000);  // 5 phút sau

        // Tạo JWT token
        String jwtToken = Jwts.builder()
                .setIssuer(clientId)  // Client ID
                .setSubject(userId)   // User ID (Subject)
                .setAudience(audience)  // Audience (Salesforce OAuth endpoint)
                .setExpiration(new Date(expTime))  // Expiration time
                .signWith(SignatureAlgorithm.RS256, privateKey)  // Ký JWT với private key
                .compact();

        return jwtToken;
    }
}
