package com.TravelShare.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class ImageOptimizationService {

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float COMPRESSION_QUALITY = 0.8f;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Tối ưu hóa hình ảnh: resize và compress
     */
    public OptimizedImage optimizeImage(MultipartFile file) throws IOException {
        try {
            // Đọc ảnh gốc
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new IOException("Không thể đọc file ảnh");
            }

            log.info("Original image: {}x{}, size: {} bytes", 
                    originalImage.getWidth(), originalImage.getHeight(), file.getSize());

            // Resize nếu cần
            BufferedImage resizedImage = resizeIfNeeded(originalImage);
            
            // Compress ảnh
            byte[] optimizedBytes = compressImage(resizedImage, COMPRESSION_QUALITY);
            
            // Tạo InputStream từ optimized bytes
            InputStream optimizedStream = new ByteArrayInputStream(optimizedBytes);
            
            // Tạo cache key
            String cacheKey = generateCacheKey(optimizedBytes);
            
            log.info("Optimized image: {}x{}, size: {} bytes (reduced by {}%)", 
                    resizedImage.getWidth(), resizedImage.getHeight(), 
                    optimizedBytes.length, 
                    ((file.getSize() - optimizedBytes.length) * 100 / file.getSize()));

            return new OptimizedImage(optimizedStream, optimizedBytes.length, cacheKey);
            
        } catch (Exception e) {
            log.error("Error optimizing image: {}", e.getMessage());
            throw new IOException("Không thể tối ưu hóa hình ảnh", e);
        }
    }

    /**
     * Resize ảnh nếu vượt quá kích thước tối đa
     */
    private BufferedImage resizeIfNeeded(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Kiểm tra xem có cần resize không
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return original;
        }
        
        // Tính toán kích thước mới giữ nguyên tỷ lệ
        double scale = Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        log.info("Resizing from {}x{} to {}x{}", width, height, newWidth, newHeight);
        
        // Tạo ảnh mới với kích thước đã resize
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        
        // Cải thiện chất lượng render
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }

    /**
     * Compress ảnh với chất lượng được chỉ định
     */
    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Lấy ImageWriter cho JPEG
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        // Thiết lập compression
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        
        // Ghi ảnh
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        
        // Cleanup
        writer.dispose();
        ios.close();
        
        return baos.toByteArray();
    }

    /**
     * Tạo cache key từ nội dung ảnh
     */
    public String generateCacheKey(byte[] imageBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(imageBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating cache key", e);
            // Fallback: sử dụng hashCode
            return String.valueOf(java.util.Arrays.hashCode(imageBytes));
        }
    }

    /**
     * Kiểm tra file có phải là ảnh không
     */
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Kiểm tra kích thước file
     */
    public boolean isValidSize(MultipartFile file) {
        return file.getSize() <= MAX_FILE_SIZE;
    }

    /**
     * Class để chứa ảnh đã tối ưu
     */
    public static class OptimizedImage {
        private final InputStream imageStream;
        private final long size;
        private final String cacheKey;

        public OptimizedImage(InputStream imageStream, long size, String cacheKey) {
            this.imageStream = imageStream;
            this.size = size;
            this.cacheKey = cacheKey;
        }

        public InputStream getImageStream() {
            return imageStream;
        }

        public long getSize() {
            return size;
        }

        public String getCacheKey() {
            return cacheKey;
        }
    }
} 