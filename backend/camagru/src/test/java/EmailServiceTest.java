import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.camagru.services.EmailService;

public class EmailServiceTest {
    @Test
    public void testSend() {
        try {

            EmailService service = EmailService.create();
            String emailTemplate = """
                    <html>
                    <head>
                      <style>
                        body {
                          font-family: Arial, sans-serif;
                          color: #333;
                          line-height: 1.6;
                        }
                        .container {
                          padding: 20px;
                          background-color: #f4f4f4;
                          border-radius: 10px;
                          max-width: 600px;
                          margin: 0 auto;
                        }
                        .btn {
                          display: inline-block;
                          padding: 10px 20px;
                          font-size: 16px;
                          color: #fff;
                          background-color: #007bff;
                          text-decoration: none;
                          border-radius: 5px;
                        }
                        .footer {
                          margin-top: 20px;
                          font-size: 12px;
                          color: #777;
                        }
                      </style>
                    </head>
                    <body>
                      <div class='container'>
                        <p>Hello,</p>
                        <p>We received a request to reset your password. Please click the button below to reset it:</p>
                        <p>
                          <a href='%s' class='btn'>Reset Password</a>
                        </p>
                        <p>If you did not request this change, you can safely ignore this email.</p>
                        <p>Thank you, <br> The Camagru Team</p>
                        <div class='footer'>
                          <p>If you have any issues, contact our support team at support@camagru.xyz.</p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """;

            String resetPasswordLink = "https://yourwebsite.com/reset-password-link";
            String formattedEmail = String.format(emailTemplate, resetPasswordLink);
            service.send("Stefan", "stefanrab465@gmail.com", "Test Email", formattedEmail);
        } catch (Exception e) {
            fail("Should not have thrown any exception");

        }
    }
}
