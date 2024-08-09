import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.camagru.JwtManager;

public class JwtManagerTest {
    @Test
    public void testCreateToken() {
        try {
            JwtManager jwtManager = new JwtManager("secret");
            String token = jwtManager.createToken("user007");
            JSONObject decoded = jwtManager.decodeToken(token);
            System.out.println(token);
            assertEquals("user007", decoded.getJSONObject("payload").getString("sub"));
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void testVerifyOwnSignature() {
        try {
            JwtManager jwtManager = new JwtManager("secret");
            String token = jwtManager.createToken("user007");
            String unverifiedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            Boolean isVerified = jwtManager.verifySignature(token);
            Boolean isVerified2 = jwtManager.verifySignature(unverifiedToken);

            assertEquals(true, isVerified);
            assertEquals(false, isVerified2);

        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void testVerifyExternalSignature() {
        try {
            JwtManager jwtManager = new JwtManager("your-256-bit-secret");
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String unverifiedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.XbPfbIHMI6arZ3Y922BhjWgQzWXcXNrz0ogtVhfEd2o";
            Boolean isVerified = jwtManager.verifySignature(token);
            Boolean isVerified2 = jwtManager.verifySignature(unverifiedToken);

            assertEquals(true, isVerified);
            assertEquals(false, isVerified2);

        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }
}
