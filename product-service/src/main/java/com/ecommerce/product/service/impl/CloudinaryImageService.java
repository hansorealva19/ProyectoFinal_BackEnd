package com.ecommerce.product.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageService {

    private final Cloudinary cloudinary;

    public Map<String, Object> upload(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        try {
            Map<String, Object> options = ObjectUtils.asMap("folder", folder == null ? "products" : folder);
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            log.info("Uploaded image to Cloudinary: {}", result.get("secure_url"));
            return result;
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }
}
