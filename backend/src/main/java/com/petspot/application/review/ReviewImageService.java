package com.petspot.application.review;

import com.petspot.api.review.dto.ReviewImageUploadResponseDto;
import com.petspot.global.error.ErrorCode;
import com.petspot.global.error.exception.ReviewImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 리뷰 사진 저장·조회 (리뷰당 1장).
 *
 * <p>올린 파일은 그대로 저장하지 않고 이미지로 읽어 다시 JPEG로 만든다. 이미지가 아닌 파일을 걸러 내고,
 * 촬영 위치(GPS) 같은 사진 속 부가 정보도 함께 지워진다. 화면에서 1600px 이하 JPEG로 줄여 올리지만
 * 서버도 같은 한도로 다시 줄인다.
 * <ul>
 *   <li>원본: 긴 변 1600px 이하 → {@code {id}.jpg} (크게 보기 팝업용)</li>
 *   <li>썸네일: 가운데를 정사각형으로 잘라 240px → {@code {id}_thumb.jpg} (리뷰 목록용)</li>
 * </ul>
 * 주소는 {@code /api/v1/review-images/{파일명}}이며, 리뷰에는 원본 주소만 저장하고 썸네일 주소는 이름 규칙으로 만든다.
 */
@Slf4j
@Service
public class ReviewImageService {

    public static final String URL_PREFIX = "/api/v1/review-images/";
    /** 리뷰에 저장할 수 있는 사진 주소 (이 서버에 올린 원본만) */
    private static final Pattern IMAGE_URL = Pattern.compile("^/api/v1/review-images/[0-9a-f]{32}\\.jpg$");
    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f]{32}(_thumb)?\\.jpg$");
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");

    static final int MAX_EDGE = 1600;
    static final int THUMB_SIZE = 240;
    /** 압축을 풀면 메모리를 많이 쓰는 이미지를 막는다 (약 4천만 화소) */
    private static final long MAX_PIXELS = 40_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    private final Path dir;

    public ReviewImageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.dir = Paths.get(uploadDir, "reviews").toAbsolutePath().normalize();
    }

    public ReviewImageUploadResponseDto store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
        }
        BufferedImage source;
        try (InputStream in = file.getInputStream()) {
            source = readImage(in);
        } catch (IOException e) {
            throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
        }

        String id = UUID.randomUUID().toString().replace("-", "");
        try {
            Files.createDirectories(dir);
            writeJpeg(scaleToFit(source, MAX_EDGE), dir.resolve(id + ".jpg"));
            writeJpeg(cropSquare(source, THUMB_SIZE), dir.resolve(id + "_thumb.jpg"));
        } catch (IOException e) {
            throw new UncheckedIOException("리뷰 사진 저장 실패", e);
        }
        log.info("[REVIEW IMAGE STORED] id: {}, source: {}x{}", id, source.getWidth(), source.getHeight());
        return new ReviewImageUploadResponseDto(URL_PREFIX + id + ".jpg", URL_PREFIX + id + "_thumb.jpg");
    }

    public Resource load(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            throw new ReviewImageException(ErrorCode.REVIEW_IMAGE_NOT_FOUND);
        }
        Path path = dir.resolve(fileName);
        if (!Files.isRegularFile(path)) {
            throw new ReviewImageException(ErrorCode.REVIEW_IMAGE_NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    /** 리뷰에 저장해도 되는 사진 주소인지 (비어 있으면 사진 없음으로 허용) */
    public static boolean isAllowedImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() || IMAGE_URL.matcher(imageUrl).matches();
    }

    /** 원본 주소 → 썸네일 주소. 이 서버에 올린 사진이 아니면(이전 데이터) 원본 주소를 그대로 쓴다 */
    public static String thumbnailUrlOf(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return IMAGE_URL.matcher(imageUrl).matches() ? imageUrl.replace(".jpg", "_thumb.jpg") : imageUrl;
    }

    private BufferedImage readImage(InputStream in) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) {
                throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                if (!ALLOWED_FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
                }
                reader.setInput(iis, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS) {
                    throw new ReviewImageException(ErrorCode.INVALID_REVIEW_IMAGE);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /** 긴 변이 maxEdge를 넘으면 비율을 지켜 줄인다 */
    static BufferedImage scaleToFit(BufferedImage src, int maxEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min(1.0, (double) maxEdge / Math.max(w, h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));
        return resize(toRgb(src), tw, th);
    }

    /** 가운데를 정사각형으로 잘라 size로 줄인다 */
    static BufferedImage cropSquare(BufferedImage src, int size) {
        int side = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - side) / 2;
        int y = (src.getHeight() - side) / 2;
        BufferedImage square = toRgb(src).getSubimage(x, y, side, side);
        return resize(square, Math.min(size, side), Math.min(size, side));
    }

    /** 투명 배경(PNG)은 흰색으로 채운 RGB로 바꾼다 (JPEG는 투명도가 없음) */
    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    /** 반씩 여러 번 줄여 한 번에 크게 줄일 때 생기는 계단 현상을 줄인다 */
    private static BufferedImage resize(BufferedImage src, int tw, int th) {
        BufferedImage current = src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == tw && h == th) {
            return src;
        }
        do {
            w = Math.max(tw, w / 2);
            h = Math.max(th, h / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(current, 0, 0, w, h, null);
            g.dispose();
            current = next;
        } while (w != tw || h != th);
        return current;
    }

    private static void writeJpeg(BufferedImage image, Path path) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
