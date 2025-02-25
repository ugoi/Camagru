import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.camagru.request_handlers.HttpUtil;

public class HttpUtilTest {
    @Test
    public void testGetHeader() {
        String header = "name=\"file\"; filename=\"file.txt\"";
        String name = "name";
        String expected = "\"file\"";
        String actual = HttpUtil.getHeader(header, name);
        assertEquals(expected, actual);
    }

    @Test
    public void testFiles() {
        byte[] boundary = "boundary".getBytes();
        InputStream inputStream = new ByteArrayInputStream("".getBytes());
        HashMap<String, byte[]> expected = new HashMap<String, byte[]>();
        HashMap<String, byte[]> actual = HttpUtil.files(inputStream, boundary);
        assertEquals(expected, actual);
    }

    // Lines should end with CRLF
    public static final String MULTIPART_BODY = "Content-Type: multipart/form-data; boundary=--AaB03x\r\n"
            + "\r\n"
            + "----AaB03x\r\n"
            + "Content-Disposition: form-data; name=\"submit-name\"\r\n"
            + "\r\n"
            + "Larry\r\n"
            + "----AaB03x\r\n"
            + "Content-Disposition: form-data; name=\"files\"; filename=\"file1.txt\"\r\n"
            + "Content-Type: text/plain\r\n"
            + "\r\n"
            + "HELLO WORLD!\r\n"
            + "----AaB03x--\r\n";

    @Test
    public void testFilesWithMultipart() {
        byte[] boundary = "--AaB03x".getBytes();
        InputStream inputStream = new ByteArrayInputStream(MULTIPART_BODY.getBytes());
        HashMap<String, byte[]> expected = new HashMap<String, byte[]>();
        expected.put("submit-name", "Larry".getBytes());
        expected.put("files", "HELLO WORLD!".getBytes());
        HashMap<String, byte[]> actual = HttpUtil.files(inputStream, boundary);
        assertTrue(Arrays.equals(expected.get("submit-name"), actual.get("submit-name")));
    }

    @Test
    public void testFilesWithMultipart2() {
        byte[] test = "HELLO WORLD!".getBytes();
        byte[] test2 = "HELLO WORLD!".getBytes();

        assertTrue(Arrays.equals(test, test2));
    }

}
