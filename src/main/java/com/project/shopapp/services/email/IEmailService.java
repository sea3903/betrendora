package com.project.shopapp.services.email;

public interface IEmailService {
    /**
     * Gửi email xác thực tài khoản
     * @param to Địa chỉ email người nhận
     * @param token Token xác thực
     */
    void sendVerificationEmail(String to, String token);
    
    /**
     * Gửi email đặt lại mật khẩu
     * @param to Địa chỉ email người nhận
     * @param token Token reset password
     */
    void sendPasswordResetEmail(String to, String token);
}
