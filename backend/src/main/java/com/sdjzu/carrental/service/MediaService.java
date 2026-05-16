package com.sdjzu.carrental.service;

import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.model.vo.MediaFileVO;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class MediaService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.media.base-dir:./uploads}")
    private String mediaBaseDir;

    @Value("${app.media.access-prefix:/static}")
    private String mediaAccessPrefix;

    public MediaFileVO uploadCarImage(MultipartFile file, String carNo) {
        SecurityUtils.requireAdmin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        validateFileSize(file.getSize());
        String extension = getExtension(file.getOriginalFilename());
        validateExtension(extension);
        String contentType = file.getContentType();
        validateContentType(contentType);
        String relativePath = buildRelativePath(extension, carNo);
        Path target = resolveTargetPath(relativePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (Exception e) {
            throw new BusinessException("图片上传失败");
        }
        return new MediaFileVO(buildAccessUrl(relativePath), target.getFileName().toString(), file.getSize());
    }

    public MediaFileVO importCarImage(String url, String carNo) {
        SecurityUtils.requireAdmin();
        if (!StringUtils.hasText(url) || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException("图片链接必须以 http 或 https 开头");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .header(HttpHeaders.USER_AGENT, "Claude-CarRental/1.0")
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("图片下载失败，远程服务器返回异常状态");
            }
            String contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse("");
            String normalizedContentType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            validateContentType(normalizedContentType);
            long contentLength = response.headers().firstValueAsLong(HttpHeaders.CONTENT_LENGTH).orElse(-1L);
            if (contentLength > 0) {
                validateFileSize(contentLength);
            }
            String extension = extensionFromContentType(normalizedContentType);
            String relativePath = buildRelativePath(extension, carNo);
            Path target = resolveTargetPath(relativePath);
            Files.createDirectories(target.getParent());
            try (InputStream body = response.body()) {
                long copied = Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
                validateFileSize(copied);
                return new MediaFileVO(buildAccessUrl(relativePath), target.getFileName().toString(), copied);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("图片下载失败");
        }
    }

    public void deleteCarImage(String imageUrl) {
        SecurityUtils.requireAdmin();
        deleteCarImageIfManaged(imageUrl);
    }

    public void deleteCarImageFolder(String carNo) {
        if (!StringUtils.hasText(carNo)) {
            return;
        }
        String folderRelativePath = Paths.get("car-images", normalizeCarFolder(carNo)).toString().replace('\\', '/');
        Path folder = resolveTargetPath(folderRelativePath);
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> pathStream = Files.walk(folder)) {
            for (Path path : pathStream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            throw new BusinessException("删除车辆图片目录失败");
        }
    }

    public String moveCarImageToCarFolder(String imageUrl, String carNo) {
        if (!StringUtils.hasText(imageUrl) || !StringUtils.hasText(carNo)) {
            return imageUrl;
        }
        String relativePath = extractRelativePath(imageUrl);
        if (!StringUtils.hasText(relativePath)) {
            return imageUrl;
        }
        String targetDirectory = "car-images/" + normalizeCarFolder(carNo) + "/";
        if (relativePath.startsWith(targetDirectory)) {
            return buildAccessUrl(relativePath);
        }
        Path source = resolveTargetPath(relativePath);
        if (!Files.exists(source)) {
            return imageUrl;
        }
        String targetRelativePath = targetDirectory + source.getFileName();
        Path target = resolveTargetPath(targetRelativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return buildAccessUrl(targetRelativePath);
        } catch (Exception e) {
            throw new BusinessException("图片移动失败");
        }
    }

    public void deleteCarImageIfManaged(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }
        String relativePath = extractRelativePath(imageUrl);
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        Path target = resolveTargetPath(relativePath);
        try {
            Files.deleteIfExists(target);
        } catch (Exception e) {
            throw new BusinessException("删除图片文件失败");
        }
    }

    public Path getMediaBasePath() {
        return Paths.get(mediaBaseDir).toAbsolutePath().normalize();
    }

    public String getMediaAccessPrefix() {
        return normalizedAccessPrefix();
    }

    private Path resolveTargetPath(String relativePath) {
        Path basePath = getMediaBasePath();
        Path targetPath = basePath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException("非法媒体路径");
        }
        return targetPath;
    }

    private String buildRelativePath(String extension, String carNo) {
        return Paths.get("car-images", normalizeCarFolder(carNo), UUID.randomUUID() + "." + extension)
                .toString()
                .replace('\\', '/');
    }

    private String buildAccessUrl(String relativePath) {
        return normalizedAccessPrefix() + "/" + relativePath;
    }

    private String normalizedAccessPrefix() {
        String prefix = StringUtils.hasText(mediaAccessPrefix) ? mediaAccessPrefix.trim() : "/static";
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String extractRelativePath(String imageUrl) {
        String path = imageUrl.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                path = URI.create(path).getPath();
            } catch (Exception e) {
                return null;
            }
        }
        if (!StringUtils.hasText(path)) {
            return null;
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        String accessPrefix = normalizedAccessPrefix();
        String prefixWithSlash = accessPrefix + "/";
        if (!path.startsWith(prefixWithSlash)) {
            return null;
        }
        return path.substring(prefixWithSlash.length());
    }

    private String normalizeCarFolder(String carNo) {
        if (!StringUtils.hasText(carNo)) {
            return "_temp";
        }
        String folder = carNo.trim().replaceAll("[^0-9A-Za-z_-]", "_");
        return folder.isEmpty() ? "_temp" : folder;
    }

    private void validateFileSize(long size) {
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
    }

    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp 格式图片");
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp 格式图片");
        }
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException("无法识别图片格式");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE, "image/jpg" -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException("仅支持 jpg、jpeg、png、webp 格式图片");
        };
    }
}
