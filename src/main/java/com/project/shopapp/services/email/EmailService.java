package com.project.shopapp.services.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * ═══════════════════════════════════════════════════════════════
 * EMAIL SERVICE - Gửi email xác thực và thông báo
 * ═══════════════════════════════════════════════════════════════
 * Sử dụng Spring Mail với SMTP Gmail
 */
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    // URL frontend để verify email
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Gửi email xác thực tài khoản khi đăng ký
     */
    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Trendora Fashion - Xác thực tài khoản");
            
            String verifyUrl = frontendUrl + "/verify-email?token=" + token;
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Inter', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                        .container { max-width: 500px; margin: 0 auto; background: #fff; border: 1px solid #e5e5e5; }
                        .header { background: #000; color: #fff; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase; }
                        .content { padding: 40px 30px; text-align: center; }
                        .content p { color: #333; line-height: 1.6; margin-bottom: 20px; }
                        .btn { display: inline-block; background: #545454; color: #fff; padding: 15px 40px; 
                               text-decoration: none; text-transform: uppercase; letter-spacing: 1px; font-size: 14px; }
                        .btn:hover { background: #000; }
                        .footer { padding: 20px; text-align: center; border-top: 1px solid #e5e5e5; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Trendora Fashion</h1>
                        </div>
                        <div class="content">
                            <p>Chào bạn,</p>
                            <p>Cảm ơn bạn đã đăng ký tài khoản tại Trendora Fashion.<br>
                            Vui lòng click vào nút bên dưới để xác thực email của bạn:</p>
                            <a href="%s" class="btn">Xác Thực Email</a>
                            <p style="margin-top: 30px; font-size: 13px; color: #666;">
                                Link này sẽ hết hạn sau 24 giờ.<br>
                                Nếu bạn không đăng ký tài khoản, vui lòng bỏ qua email này.
                            </p>
                        </div>
                        <div class="footer">
                            © 2024 Trendora Fashion. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(verifyUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email xác thực: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gửi email đặt lại mật khẩu
     */
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Trendora Fashion - Đặt lại mật khẩu");
            
            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Inter', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                        .container { max-width: 500px; margin: 0 auto; background: #fff; border: 1px solid #e5e5e5; }
                        .header { background: #000; color: #fff; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; letter-spacing: 2px; text-transform: uppercase; }
                        .content { padding: 40px 30px; text-align: center; }
                        .content p { color: #333; line-height: 1.6; margin-bottom: 20px; }
                        .btn { display: inline-block; background: #545454; color: #fff; padding: 15px 40px; 
                               text-decoration: none; text-transform: uppercase; letter-spacing: 1px; font-size: 14px; }
                        .footer { padding: 20px; text-align: center; border-top: 1px solid #e5e5e5; color: #999; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Trendora Fashion</h1>
                        </div>
                        <div class="content">
                            <p>Chào bạn,</p>
                            <p>Bạn đã yêu cầu đặt lại mật khẩu.<br>
                            Click vào nút bên dưới để tạo mật khẩu mới:</p>
                            <a href="%s" class="btn">Đặt Lại Mật Khẩu</a>
                            <p style="margin-top: 30px; font-size: 13px; color: #666;">
                                Link này sẽ hết hạn sau 1 giờ.<br>
                                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                            </p>
                        </div>
                        <div class="footer">
                            © 2024 Trendora Fashion. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetUrl);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu: " + e.getMessage(), e);
        }
    }
}
