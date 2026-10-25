package com.safenavi.backend.storage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Uploads report photos plus a generated JPEG thumbnail to S3. */
@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif");
    private static final int THUMBNAIL_MAX_EDGE = 320;
    private static final String PREFIX = "reports/";

    private final S3Client s3Client;
    private final S3Properties properties;

    public ImageStorageService(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public StoredImage upload(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType.toLowerCase());
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "JPEG, PNG, GIF 이미지만 업로드할 수 있습니다.");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "이미지를 읽을 수 없습니다.", e);
        }
        byte[] thumbnail = createThumbnail(original);

        String id = UUID.randomUUID().toString();
        String imageKey = PREFIX + id + "." + extension;
        String thumbnailKey = PREFIX + "thumbnails/" + id + ".jpg";
        put(imageKey, original, contentType);
        try {
            put(thumbnailKey, thumbnail, "image/jpeg");
        } catch (BusinessException e) {
            deleteQuietly(urlOf(imageKey));
            throw e;
        }
        return new StoredImage(urlOf(imageKey), urlOf(thumbnailKey));
    }

    /** Best-effort cleanup; a leftover object is not worth failing the request for. */
    public void delete(StoredImage image) {
        if (image == null) {
            return;
        }
        deleteQuietly(image.imageUrl());
        deleteQuietly(image.thumbnailUrl());
    }

    private void put(String key, byte[] bytes, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(properties.s3().bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "이미지 업로드에 실패했습니다.", e);
        }
    }

    private void deleteQuietly(String url) {
        String key = keyOf(url);
        if (key == null) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.s3().bucket()).key(key).build());
        } catch (SdkException e) {
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private String baseUrl() {
        String custom = properties.s3().publicBaseUrl();
        if (custom != null && !custom.isBlank()) {
            return custom.endsWith("/") ? custom : custom + "/";
        }
        return "https://" + properties.s3().bucket() + ".s3." + properties.s3().region() + ".amazonaws.com/";
    }

    private String urlOf(String key) {
        return baseUrl() + key;
    }

    private String keyOf(String url) {
        if (url == null || !url.startsWith(baseUrl())) {
            return null;
        }
        return url.substring(baseUrl().length());
    }

    static byte[] createThumbnail(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new java.io.ByteArrayInputStream(original));
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE, "이미지를 해석할 수 없습니다.");
            }
            double scale = Math.min(1.0, (double) THUMBNAIL_MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setColor(java.awt.Color.WHITE); // flatten PNG transparency for JPEG
                g.fillRect(0, 0, width, height);
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(thumb, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "이미지를 해석할 수 없습니다.", e);
        }
    }
}
