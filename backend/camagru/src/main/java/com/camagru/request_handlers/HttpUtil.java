package com.camagru.request_handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.tomcat.util.http.fileupload.MultipartStream;

/**
 * Utility class for handling HTTP requests.
 */
public class HttpUtil {
    /**
     * Extracts the value of a header from a header string.
     * 
     * @param cookieHeader The header string
     * @param name         The name of the header
     * @return The value of the header
     */
    public static String getHeader(String cookieHeader, String name) {
        String cookieString = "; " + cookieHeader;
        String[] parts = cookieString.split("; " + name + "=");
        if (parts.length == 2) {
            return parts[1].split(";")[0];
        }
        throw new IllegalArgumentException("Value not found");

    }

    /**
     * Extracts the files from the input stream of a multipart request.
     * 
     * @param inputStream The input stream of multipart request body
     * @return A hashmap containing the files
     */
    public static HashMap<String, byte[]> files(InputStream inputStream, byte[] boundary) {
        HashMap<String, byte[]> jsonParts = new HashMap<String, byte[]>();
        try {

            MultipartStream multipartStream = new MultipartStream(inputStream, boundary, 8192, null);
            boolean nextPart = multipartStream.skipPreamble();
            // OutputStream output;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // ObjectOutputStream output = new ObjectOutputStream(baos);

            while (nextPart) {
                String header = multipartStream.readHeaders().trim();
                // create some output stream
                multipartStream.readBodyData(output);
                byte[] multipartData = output.toByteArray();

                // Print multipart data as string
                String multipartDataString = new String(multipartData);
                System.out.println(multipartDataString);
                // process headers
                try {
                    String headerValueRaw = HttpUtil.getHeader(header, "name");

                    String headerValue = headerValueRaw.replace("\"", "");

                    if (!headerValue.isBlank()) {
                        jsonParts.put(headerValue, multipartData);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
                nextPart = multipartStream.readBoundary();
            }
        } catch (MultipartStream.MalformedStreamException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return jsonParts;
    }
}
