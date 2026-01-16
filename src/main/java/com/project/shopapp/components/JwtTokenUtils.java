package com.project.shopapp.components;
import com.project.shopapp.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.shopapp.exceptions.InvalidParamException;
import com.project.shopapp.models.Token;
import com.project.shopapp.repositories.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

import io.jsonwebtoken.Jwts.*;

@Component
@RequiredArgsConstructor
public class JwtTokenUtils {
    @Value("${jwt.expiration}")
    private int expiration; //save to an environment variable

    @Value("${jwt.expiration-refresh-token}")
    private int expirationRefreshToken;

    @Value("${jwt.secretKey}")
    private String secretKey;
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtils.class);
    private final TokenRepository tokenRepository;
    public String generateToken(com.project.shopapp.models.User user) throws Exception{
        //properties => claims
        Map<String, Object> claims = new HashMap<>();
        // Dùng userId làm subject - không bao giờ thay đổi dù user đổi phone/email
        String subject = String.valueOf(user.getId());
        claims.put("userId", user.getId());
        // Lưu thêm phone/email vào claims để tham khảo (không dùng làm subject)
        claims.put("phoneNumber", user.getPhoneNumber());
        claims.put("email", user.getEmail());
        try {
            String token = Jwts.builder()
                    .claims(claims)
                    .subject(subject) // subject = userId (string)
                    .expiration(new Date(System.currentTimeMillis() + expiration * 1000L))
                    .signWith(getSignInKey(), Jwts.SIG.HS256)
                    .compact();
            return token;
        }catch (Exception e) {
            throw new InvalidParamException("Cannot create jwt token, error: "+e.getMessage());
        }
    }
    // Lấy userId từ token
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Ưu tiên lấy userId từ claims (cả token cũ và mới đều có)
            Object userId = claims.get("userId");
            if (userId != null) {
                Long id = Long.valueOf(userId.toString());
                logger.debug("Got userId from claims: {}", id);
                return id;
            }
            
            // Fallback: thử parse subject (chỉ token mới có subject = userId)
            String subject = claims.getSubject();
            logger.debug("Trying to parse subject as userId: {}", subject);
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            // Subject là phone/email (token cũ) - không parse được
            logger.warn("Subject is not a valid userId (old token format): {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Cannot get userId from token: {}", e.getMessage());
            return null;
        }
    }
    private SecretKey getSignInKey() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        //Keys.hmacShaKeyFor(Decoders.BASE64.decode("TaqlmGv1iEDMRiFp/pHuID1+T84IABfuA0xXh4GhiUI="));
        return Keys.hmacShaKeyFor(bytes);
    }


    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        random.nextBytes(keyBytes);
        String secretKey = Encoders.BASE64.encode(keyBytes);
        return secretKey;
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()  // Khởi tạo JwtParserBuilder
                .verifyWith(getSignInKey())  // Sử dụng verifyWith() để thiết lập signing key
                .build()  // Xây dựng JwtParser
                .parseSignedClaims(token)  // Phân tích token đã ký
                .getPayload();  // Lấy phần body của JWT, chứa claims
    }



    public  <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    //check expiration
    public boolean isTokenExpired(String token) {
        Date expirationDate = this.extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }
    public String getSubject(String token) {
        return  extractClaim(token, Claims::getSubject);
    }
    public boolean validateToken(String token, User userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            String subject = claims.getSubject();
            
            // Kiểm tra token có trong DB và chưa bị revoke
            Token existingToken = tokenRepository.findByToken(token);
            if (existingToken == null ||
                    existingToken.isRevoked() ||
                    !userDetails.isActive()
            ) {
                return false;
            }
            
            // Kiểm tra hết hạn
            if (isTokenExpired(token)) {
                return false;
            }
            
            // Thử lấy userId từ claims (cả token cũ và mới đều có field này)
            Object userId = claims.get("userId");
            if (userId != null) {
                Long tokenUserId = Long.valueOf(userId.toString());
                return tokenUserId.equals(userDetails.getId());
            }
            
            // Fallback cho token cũ: subject là phone hoặc email
            return subject.equals(userDetails.getUsername()) || 
                   subject.equals(userDetails.getPhoneNumber()) ||
                   subject.equals(userDetails.getEmail());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
        }

        return false;
    }
}
