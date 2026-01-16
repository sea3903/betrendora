package com.project.shopapp.filters;

import com.project.shopapp.components.JwtTokenUtils;
import com.project.shopapp.models.User;
import com.project.shopapp.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    @Value("${api.prefix}")
    private String apiPrefix;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtils jwtTokenUtil;
    private final UserRepository userRepository; // Thêm để tìm user bằng ID

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (isBypassToken(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJsonError(response, "Token không hợp lệ hoặc thiếu");
                return;
            }
            
            final String token = authHeader.substring(7);
            
            // Lấy userId từ token (token mới có subject = userId)
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            User userDetails = null;
            
            if (userId != null) {
                // Token mới: tìm user bằng ID
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    userDetails = userOpt.get();
                }
            } else {
                // Token cũ: subject là phone/email, thử tìm bằng phone hoặc email
                String subject = jwtTokenUtil.getSubject(token);
                if (subject != null) {
                    Optional<User> userByPhone = userRepository.findByPhoneNumber(subject);
                    if (userByPhone.isPresent()) {
                        userDetails = userByPhone.get();
                    } else {
                        Optional<User> userByEmail = userRepository.findByEmail(subject);
                        if (userByEmail.isPresent()) {
                            userDetails = userByEmail.get();
                        }
                    }
                }
            }
            
            if (userDetails != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtTokenUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } else if (userDetails == null) {
                sendJsonError(response, "Người dùng không tồn tại. Vui lòng đăng nhập lại.");
                return;
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendJsonError(response, e.getMessage());
        }
    }
    
    // Trả về JSON error response
    private void sendJsonError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String safeMessage = message != null ? message.replace("\"", "\\\"") : "Lỗi xác thực";
        String jsonError = String.format("{\"message\": \"%s\", \"status\": 401}", safeMessage);
        response.getWriter().write(jsonError);
    }

    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                // Healthcheck request, no JWT token required
                Pair.of(String.format("%s/healthcheck/health", apiPrefix), "GET"),
                Pair.of(String.format("%s/actuator/**", apiPrefix), "GET"),

                Pair.of(String.format("%s/roles**", apiPrefix), "GET"),
                Pair.of(String.format("%s/policies**", apiPrefix), "GET"),
                Pair.of(String.format("%s/comments**", apiPrefix), "GET"),
                Pair.of(String.format("%s/coupons**", apiPrefix), "GET"),

                Pair.of(String.format("%s/products**", apiPrefix), "GET"),
                Pair.of(String.format("%s/categories**", apiPrefix), "GET"),

                Pair.of(String.format("%s/users/register", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/login", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/verify", apiPrefix), "GET"),
                Pair.of(String.format("%s/users/profile-images/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/users/forgot-password", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/reset-password-token", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/refreshToken", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/auth/social/callback", apiPrefix), "GET"),

                // Swagger
                Pair.of("/api-docs", "GET"),
                Pair.of("/api-docs/**", "GET"),
                Pair.of("/swagger-resources", "GET"),
                Pair.of("/swagger-resources/**", "GET"),
                Pair.of("/configuration/ui", "GET"),
                Pair.of("/configuration/security", "GET"),
                Pair.of("/swagger-ui/**", "GET"),
                Pair.of("/swagger-ui.html", "GET"),
                Pair.of("/swagger-ui/index.html", "GET"),

                // Đăng nhập social
                Pair.of(String.format("%s/users/auth/social-login**", apiPrefix), "GET"),
                Pair.of(String.format("%s/users/auth/social/callback**", apiPrefix), "GET"),

                Pair.of(String.format("%s/payments**", apiPrefix), "GET"),
                Pair.of(String.format("%s/payments**", apiPrefix), "POST")
        );

        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        for (Pair<String, String> token : bypassTokens) {
            String path = token.getFirst();
            String method = token.getSecond();
            if (requestPath.matches(path.replace("**", ".*"))
                    && requestMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }
}
