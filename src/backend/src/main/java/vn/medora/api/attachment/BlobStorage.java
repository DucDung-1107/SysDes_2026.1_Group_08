package vn.medora.api.attachment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import static vn.medora.api.common.ApiSupport.*;

/** Private object storage. The filesystem driver is for local development only. */
@Component
public class BlobStorage implements AutoCloseable {
    private final Path root; private final S3Client s3; private final String bucket;
    public BlobStorage(@Value("${medora.storage.driver:filesystem}") String driver,
        @Value("${medora.storage.path:./data/attachments}") String directory,
        @Value("${medora.storage.s3.endpoint:}") String endpoint,
        @Value("${medora.storage.s3.region:us-east-1}") String region,
        @Value("${medora.storage.s3.bucket:medora-private}") String bucket,
        @Value("${medora.storage.s3.access-key:}") String access,
        @Value("${medora.storage.s3.secret-key:}") String secret) {
        this.root=Path.of(directory).toAbsolutePath().normalize(); this.bucket=bucket;
        if(driver.equals("s3")) {
            var builder=S3Client.builder().region(Region.of(region)).forcePathStyle(true)
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(15)));
            if(!endpoint.isBlank()) { builder.endpointOverride(URI.create(endpoint)); }
            if(!access.isBlank() && !secret.isBlank()) { builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret))); }
            else { builder.credentialsProvider(DefaultCredentialsProvider.builder().build()); }
            s3=builder.build();
        } else if(driver.equals("filesystem")) { s3=null; }
        else { throw new IllegalArgumentException("Unsupported storage driver"); }
    }
    private Path path(String key) {
        if(!key.matches("[a-f0-9-]{36}\\.blob")) { throw bad("INVALID_STORAGE_KEY"); }
        Path result=root.resolve(key).normalize(); if(!result.startsWith(root)) { throw bad("INVALID_STORAGE_KEY"); } return result;
    }
    public void put(String key,byte[] data,String type) throws IOException {
        if(s3!=null) { s3.putObject(r->r.bucket(bucket).key(key).contentType(type),RequestBody.fromBytes(data)); }
        else { Files.createDirectories(root); Files.write(path(key),data,StandardOpenOption.CREATE_NEW); }
    }
    public byte[] get(String key) throws IOException {
        try(InputStream input=s3==null?Files.newInputStream(path(key)):s3.getObject(r->r.bucket(bucket).key(key))) {
            byte[] bytes=input.readNBytes(10*1024*1024+1);
            if(bytes.length>10*1024*1024) { throw new IOException("Stored object exceeds the configured limit"); } return bytes;
        }
    }
    public void removeUncommitted(String key) {
        try { if(s3==null) { Files.deleteIfExists(path(key)); } else { s3.deleteObject(r->r.bucket(bucket).key(key)); } }
        catch(Exception ignored) { /* A failed cleanup is an orphaned object, never an authorized attachment. */ }
    }
    @Override public void close() { if(s3!=null) { s3.close(); } }
}
