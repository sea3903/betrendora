package com.project.shopapp.services.user;

import com.project.shopapp.components.JwtTokenUtils;
import com.project.shopapp.components.LocalizationUtils;
import com.project.shopapp.dtos.UpdateUserDTO;
import com.project.shopapp.dtos.UserDTO;
import com.project.shopapp.dtos.UserLoginDTO;
import com.project.shopapp.exceptions.DataNotFoundException;

import com.project.shopapp.exceptions.ExpiredTokenException;
import com.project.shopapp.exceptions.InvalidPasswordException;
import com.project.shopapp.exceptions.PermissionDenyException;
import com.project.shopapp.models.*;
import com.project.shopapp.repositories.RoleRepository;
import com.project.shopapp.repositories.TokenRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.utils.MessageKeys;
import com.project.shopapp.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static com.project.shopapp.utils.ValidationUtils.isValidEmail;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtils jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final LocalizationUtils localizationUtils;
    private final com.project.shopapp.services.email.IEmailService emailService;

    @Override
    @Transactional
    public User createUser(UserDTO userDTO) throws Exception {
        // Email là bắt buộc cho đăng ký
        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            throw new DataIntegrityViolationException("Email is required");
        }
        
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }
        
        // Phone là optional
        if (userDTO.getPhoneNumber() != null && !userDTO.getPhoneNumber().isBlank() 
                && userRepository.existsByPhoneNumber(userDTO.getPhoneNumber())) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }
        
        Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataNotFoundException(
                        localizationUtils.getLocalizedMessage(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        if (role.getName().equalsIgnoreCase(Role.ADMIN)) {
            throw new PermissionDenyException("Registering admin accounts is not allowed");
        }
        
        // Tạo user mới với active = false (chưa xác thực email)
        User newUser = User.builder()
                .fullName(userDTO.getFullName())
                .phoneNumber(userDTO.getPhoneNumber() != null ? userDTO.getPhoneNumber() : "")
                .email(userDTO.getEmail())
                .password(userDTO.getPassword())
                .address(userDTO.getAddress())
                .dateOfBirth(userDTO.getDateOfBirth())
                .facebookAccountId(userDTO.getFacebookAccountId())
                .googleAccountId(userDTO.getGoogleAccountId())
                .active(false) // Chưa kích hoạt, chờ xác thực email
                .build();

        newUser.setRole(role);

        if (!userDTO.isSocialLogin()) {
            String password = userDTO.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            newUser.setPassword(encodedPassword);
        }
        
        User savedUser = userRepository.save(newUser);
        
        // Tạo verification token và gửi email
        String verificationToken = UUID.randomUUID().toString();
        Token token = Token.builder()
                .token(verificationToken)
                .tokenType("VERIFICATION")
                .user(savedUser)
                .expirationDate(LocalDateTime.now().plusHours(24)) // Hết hạn sau 24h
                .revoked(false)
                .expired(false)
                .build();
        tokenRepository.save(token);
        
        // Gửi email xác thực
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        
        return savedUser;
    }


    @Override
    public String login(UserLoginDTO userLoginDTO) throws Exception {
        Optional<User> optionalUser = Optional.empty();

        // Kiểm tra người dùng qua số điện thoại
        if (userLoginDTO.getPhoneNumber() != null && !userLoginDTO.getPhoneNumber().isBlank()) {
            optionalUser = userRepository.findByPhoneNumber(userLoginDTO.getPhoneNumber());
        }

        // Nếu không tìm thấy người dùng bằng số điện thoại, thử tìm qua email
        if (optionalUser.isEmpty() && userLoginDTO.getEmail() != null) {
            optionalUser = userRepository.findByEmail(userLoginDTO.getEmail());
        }

        // Nếu không tìm thấy người dùng, ném ngoại lệ
        if (optionalUser.isEmpty()) {
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.WRONG_PHONE_PASSWORD));
        }

        User existingUser = optionalUser.get();
        
        // ════════════════════════════════════════════════════════════════
        // DEBUG: Log để kiểm tra giá trị
        // ════════════════════════════════════════════════════════════════
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("[LOGIN DEBUG] Email: " + existingUser.getEmail());
        System.out.println("[LOGIN DEBUG] FacebookId: [" + existingUser.getFacebookAccountId() + "]");
        System.out.println("[LOGIN DEBUG] GoogleId: [" + existingUser.getGoogleAccountId() + "]");
        System.out.println("[LOGIN DEBUG] Password from DB: " + existingUser.getPassword());
        System.out.println("[LOGIN DEBUG] Password from login: " + userLoginDTO.getPassword());
        
        boolean fbNull = existingUser.getFacebookAccountId() == null;
        boolean fbBlank = existingUser.getFacebookAccountId() != null && existingUser.getFacebookAccountId().isBlank();
        boolean ggNull = existingUser.getGoogleAccountId() == null;
        boolean ggBlank = existingUser.getGoogleAccountId() != null && existingUser.getGoogleAccountId().isBlank();
        
        System.out.println("[LOGIN DEBUG] FB null: " + fbNull + ", FB blank: " + fbBlank);
        System.out.println("[LOGIN DEBUG] GG null: " + ggNull + ", GG blank: " + ggBlank);
        
        // ════════════════════════════════════════════════════════════════
        // KIỂM TRA MẬT KHẨU - QUAN TRỌNG!
        // Chỉ skip nếu user có facebook_account_id HOẶC google_account_id thật sự
        // LƯU Ý: Database có thể lưu "0" như default value, phải check cả "0"
        // ════════════════════════════════════════════════════════════════
        boolean hasSocialAccount = false;
        
        String fbId = existingUser.getFacebookAccountId();
        String ggId = existingUser.getGoogleAccountId();
        
        // Check Facebook - must be non-null, non-blank, AND not "0"
        if (fbId != null && !fbId.isBlank() && !fbId.equals("0")) {
            hasSocialAccount = true;
        }
        // Check Google - must be non-null, non-blank, AND not "0"  
        if (ggId != null && !ggId.isBlank() && !ggId.equals("0")) {
            hasSocialAccount = true;
        }
        
        System.out.println("[LOGIN DEBUG] Has social account (after 0 check): " + hasSocialAccount);
        
        if (!hasSocialAccount) {
            // User đăng ký thường - PHẢI KIỂM TRA PASSWORD
            System.out.println("[LOGIN DEBUG] Checking password...");
            boolean passwordMatch = passwordEncoder.matches(userLoginDTO.getPassword(), existingUser.getPassword());
            System.out.println("[LOGIN DEBUG] Password match: " + passwordMatch);
            
            if (!passwordMatch) {
                System.out.println("[LOGIN DEBUG] PASSWORD MISMATCH - THROWING EXCEPTION");
                System.out.println("═══════════════════════════════════════════════════");
                throw new BadCredentialsException(localizationUtils.getLocalizedMessage(MessageKeys.WRONG_PHONE_PASSWORD));
            }
        } else {
            System.out.println("[LOGIN DEBUG] SKIPPING password check - social login user");
        }
        System.out.println("═══════════════════════════════════════════════════");

        // Kiểm tra tài khoản có bị khóa không
        if (!existingUser.isActive()) {
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.USER_IS_LOCKED));
        }

        // Tạo JWT token cho người dùng
        return jwtTokenUtil.generateToken(existingUser);
    }


    @Override
    public String loginSocial(UserLoginDTO userLoginDTO) throws Exception {
        Optional<User> optionalUser = Optional.empty();
        Role roleUser = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new DataNotFoundException(
                        localizationUtils.getLocalizedMessage(MessageKeys.ROLE_DOES_NOT_EXISTS)));

        String email = userLoginDTO.getEmail();
        
        // ═══════════════════════════════════════════════════════════════
        // BƯỚC 1: Tìm user theo Google/Facebook ID
        // ═══════════════════════════════════════════════════════════════
        if (userLoginDTO.isGoogleAccountIdValid()) {
            optionalUser = userRepository.findFirstByGoogleAccountIdOrderByIdDesc(userLoginDTO.getGoogleAccountId());
        } else if (userLoginDTO.isFacebookAccountIdValid()) {
            optionalUser = userRepository.findFirstByFacebookAccountIdOrderByIdDesc(userLoginDTO.getFacebookAccountId());
        } else {
            throw new IllegalArgumentException("Invalid social account information.");
        }

        // ═══════════════════════════════════════════════════════════════
        // BƯỚC 2: Nếu không tìm thấy, kiểm tra email có tồn tại không
        // ═══════════════════════════════════════════════════════════════
        if (optionalUser.isEmpty() && email != null && !email.isBlank()) {
            // Tìm user theo email để merge account
            optionalUser = userRepository.findFirstByEmailOrderByIdDesc(email);
            
            if (optionalUser.isPresent()) {
                // Merge: Liên kết social account vào user hiện có
                User existingUser = optionalUser.get();
                if (userLoginDTO.isGoogleAccountIdValid()) {
                    existingUser.setGoogleAccountId(userLoginDTO.getGoogleAccountId());
                } else if (userLoginDTO.isFacebookAccountIdValid()) {
                    existingUser.setFacebookAccountId(userLoginDTO.getFacebookAccountId());
                }
                // Cập nhật profile image nếu chưa có
                if (existingUser.getProfileImage() == null || existingUser.getProfileImage().isBlank()) {
                    existingUser.setProfileImage(Optional.ofNullable(userLoginDTO.getProfileImage()).orElse(""));
                }
                // Kích hoạt tài khoản (đã verify qua social)
                existingUser.setActive(true);
                userRepository.save(existingUser);
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // BƯỚC 3: Nếu vẫn không tìm thấy, tạo user mới
        // ═══════════════════════════════════════════════════════════════
        if (optionalUser.isEmpty()) {
            User newUser = User.builder()
                    .fullName(Optional.ofNullable(userLoginDTO.getFullname()).orElse(""))
                    .email(Optional.ofNullable(email).orElse(""))
                    .profileImage(Optional.ofNullable(userLoginDTO.getProfileImage()).orElse(""))
                    .role(roleUser)
                    .password("")
                    .active(true) // Social login tự động kích hoạt
                    .build();
            
            if (userLoginDTO.isGoogleAccountIdValid()) {
                newUser.setGoogleAccountId(userLoginDTO.getGoogleAccountId());
            } else if (userLoginDTO.isFacebookAccountIdValid()) {
                newUser.setFacebookAccountId(userLoginDTO.getFacebookAccountId());
            }
            
            newUser = userRepository.save(newUser);
            optionalUser = Optional.of(newUser);
        }

        User user = optionalUser.get();

        // Kiểm tra nếu tài khoản bị khóa
        if (!user.isActive()) {
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.USER_IS_LOCKED));
        }

        // Tạo JWT token cho người dùng
        return jwtTokenUtil.generateToken(user);
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFY EMAIL - Kích hoạt tài khoản qua link email
    // ═══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public boolean verifyEmail(String token) throws Exception {
        // Tìm token verification
        Token verificationToken = tokenRepository.findByTokenAndTokenType(token, "VERIFICATION");
        
        if (verificationToken == null) {
            throw new DataNotFoundException("Token không hợp lệ hoặc đã hết hạn");
        }
        
        // Kiểm tra hết hạn
        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            throw new ExpiredTokenException("Token đã hết hạn. Vui lòng đăng ký lại.");
        }
        
        // Kích hoạt user
        User user = verificationToken.getUser();
        user.setActive(true);
        userRepository.save(user);
        
        // Xóa token đã sử dụng
        tokenRepository.delete(verificationToken);
        
        return true;
    }


    @Transactional
    @Override
    public User updateUser(Long userId, UpdateUserDTO updatedUserDTO) throws Exception {
        // Find the existing user by userId
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Update user information based on the DTO - Partial Update
        if (updatedUserDTO.getFullName() != null && !updatedUserDTO.getFullName().isBlank()) {
            existingUser.setFullName(updatedUserDTO.getFullName());
        }
        
        if (updatedUserDTO.getPhoneNumber() != null 
            && !updatedUserDTO.getPhoneNumber().isBlank() 
            && !updatedUserDTO.getPhoneNumber().trim().equals(existingUser.getPhoneNumber())) {
             // Check phone uniqueness if changed
             Optional<User> phoneUser = userRepository.findByPhoneNumber(updatedUserDTO.getPhoneNumber());
             if (phoneUser.isPresent() && !phoneUser.get().getId().equals(existingUser.getId())) {
                 throw new DataIntegrityViolationException("Phone number already exists");
             }
            existingUser.setPhoneNumber(updatedUserDTO.getPhoneNumber());
        }
        
        if (updatedUserDTO.getAddress() != null && !updatedUserDTO.getAddress().isBlank()) {
            existingUser.setAddress(updatedUserDTO.getAddress());
        }
        
        if (updatedUserDTO.getDateOfBirth() != null) {
            existingUser.setDateOfBirth(updatedUserDTO.getDateOfBirth());
        }
        // Update Password if provided
        if (updatedUserDTO.getPassword() != null && !updatedUserDTO.getPassword().isBlank()) {
             // Verify current password first
             if (updatedUserDTO.getCurrentPassword() == null || updatedUserDTO.getCurrentPassword().isBlank()) {
                 throw new IllegalArgumentException("Current password is required to change password");
             }
             if (!passwordEncoder.matches(updatedUserDTO.getCurrentPassword(), existingUser.getPassword())) {
                 throw new IllegalArgumentException("Current password is incorrect");
             }
             
             // Verify new password matches confirmation
             if (updatedUserDTO.getRetypePassword() == null || 
                 !updatedUserDTO.getPassword().equals(updatedUserDTO.getRetypePassword())) {
                 throw new IllegalArgumentException("New passwords do not match");
             }
             
             // Validate password length
             if (updatedUserDTO.getPassword().length() < 6) {
                 throw new IllegalArgumentException("Password must be at least 6 characters");
             }
             
             String encodedPassword = passwordEncoder.encode(updatedUserDTO.getPassword());
             existingUser.setPassword(encodedPassword);
        }
        //existingUser.setRole(updatedRole);
        // Save the updated user
        return userRepository.save(existingUser);
    }

    @Override
    public User getUserDetailsFromToken(String token) throws Exception {
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new ExpiredTokenException("Token is expired");
        }
        
        // Thử lấy userId từ token (format mới)
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                return user.get();
            }
        }
        
        // Fallback: dùng subject (format cũ: phone/email)
        String subject = jwtTokenUtil.getSubject(token);
        Optional<User> user = userRepository.findFirstByPhoneNumberOrderByIdDesc(subject);
        if (user.isEmpty() && isValidEmail(subject)) {
            user = userRepository.findFirstByEmailOrderByIdDesc(subject);
        }
        return user.orElseThrow(() -> new Exception("User not found"));
    }
    @Override
    public User getUserDetailsFromRefreshToken(String refreshToken) throws Exception {
        Token existingToken = tokenRepository.findByRefreshToken(refreshToken);
        return getUserDetailsFromToken(existingToken.getToken());
    }

    @Override
    public Page<User> findAll(String keyword, Pageable pageable) {
        return userRepository.findAll(keyword, pageable);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword)
            throws InvalidPasswordException, DataNotFoundException {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));
        String encodedPassword = passwordEncoder.encode(newPassword);
        existingUser.setPassword(encodedPassword);
        userRepository.save(existingUser);
        //reset password => clear token
        List<Token> tokens = tokenRepository.findByUser(existingUser);
        for (Token token : tokens) {
            tokenRepository.delete(token);
        }
    }

    @Override
    @Transactional
    public void blockOrEnable(Long userId, Boolean active) throws DataNotFoundException {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));
        existingUser.setActive(active);
        userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void changeProfileImage(Long userId, String imageName) throws Exception {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));
        existingUser.setProfileImage(imageName);
        userRepository.save(existingUser);
    }


    @Override
    @Transactional
    public void sendResetPasswordEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Generate Token
        String tokenString = UUID.randomUUID().toString();
        Token token = Token.builder()
                .token(tokenString)
                .tokenType("RESET_PASSWORD")
                .user(user)
                .expirationDate(LocalDateTime.now().plusHours(1)) // 1 hour expiration
                .revoked(false)
                .expired(false)
                .isMobile(false)
                .build();
        tokenRepository.save(token);

        // Send Email
        emailService.sendPasswordResetEmail(email, tokenString);
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(String tokenString, String newPassword) throws Exception {
        Token token = tokenRepository.findByTokenAndTokenType(tokenString, "RESET_PASSWORD");
        
        if (token == null) {
            throw new DataNotFoundException("Invalid or expired token");
        }
        
        if (token.getExpirationDate().isBefore(LocalDateTime.now()) || token.isRevoked() || token.isExpired()) {
            throw new ExpiredTokenException("Token has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate token
        token.setRevoked(true);
        token.setExpired(true);
        tokenRepository.save(token);
    }
}




