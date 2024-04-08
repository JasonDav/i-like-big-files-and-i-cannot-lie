package com.jaysus.javastreaming.controller;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @CrossOrigin(origins = "http://localhost")
    @PostMapping(value = "/blob", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadBlob(InputStream dataStream, HttpServletResponse response) throws IOException, ServletException {

        log.debug("1. Request received at Proxy");

        // Getting the file causes the entire thing to be loaded at once before processing.
        // the lazy property does nothing to stop this and the only way to do this yourself
        // is to write your own parser.
        // Honestly pretty cringe.
        sendToDestination(dataStream);
    }

    @CrossOrigin(origins = "http://localhost")
    @PostMapping(value = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadMultipart(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        log.debug("1. Request received at Proxy");

        // Getting the file causes the entire thing to be loaded at once before processing.
        // the lazy property does nothing to stop this and the only way to do this yourself
        // is to write your own parser.
        // Honestly pretty cringe.
        Part file = request.getPart("file");
        InputStream inputStream = file.getInputStream();

        sendToDestination(inputStream);
    }

    private static void sendToDestination(InputStream inputStream) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost("http://destination:8080/receive");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            // This attaches the file to the POST:
            builder.addBinaryBody(
                    "file",
                    inputStream,
                    ContentType.DEFAULT_BINARY,
                    "file.tmp"
            );
            builder.addTextBody("type", "file");

            // Perform
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            log.debug("2. Request proxied to destination");
            CloseableHttpResponse destinationResponse = httpClient.execute(uploadFile);
            log.debug("5. Destination replied with status " + destinationResponse.getStatusLine().toString());
        }
    }

    @PostMapping(value = "/receive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void receive(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.debug("3. Request received at destination");

        Part file = request.getPart("file");

        try (InputStream inputStream = file.getInputStream()) {
            // Write to disk
            File tempFile = File.createTempFile("tmp/file", ".tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            IOUtils.copy(inputStream,fileOutputStream);
            log.debug("4. File saved at destination");
        }
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> listFiles(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        return Stream.of(new File("tmp").listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toMap(File::getName, this::getFileSizeMegaBytes));

    }

    private String getFileSizeMegaBytes(File file) {
        return (double) file.length() / (1024 * 1024) + " mb";
    }

}