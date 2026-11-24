package com.petspot.application.review;

import com.petspot.api.review.dto.ReviewImageUploadResponseDto;
import com.petspot.global.error.ErrorCode;
import com.petspot.global.error.exception.ReviewImageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewImageServiceTest {

    @TempDir
    Path tempDir;

    private static byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("사진을 올리면 긴 변 1600px 이하 JPEG와 240px 정사각형 썸네일로 다시 저장하고 주소를 돌려준다")
    void store_ResizesAndCreatesThumbnail() throws Exception {
        ReviewImageService service = new ReviewImageService(tempDir.toString());

        ReviewImageUploadResponseDto result = service.store(new MockMultipartFile("file", "dog.png", "image/png", png(3200, 1800)));

        assertThat(result.getImageUrl()).matches("^/api/v1/review-images/[0-9a-f]{32}\\.jpg$");
        assertThat(result.getThumbnailUrl()).isEqualTo(result.getImageUrl().replace(".jpg", "_thumb.jpg"));
        assertThat(ReviewImageService.thumbnailUrlOf(result.getImageUrl())).isEqualTo(result.getThumbnailUrl());
        assertThat(ReviewImageService.isAllowedImageUrl(result.getImageUrl())).isTrue();

        String name = result.getImageUrl().substring(ReviewImageService.URL_PREFIX.length());
        BufferedImage main = ImageIO.read(service.load(name).getFile());
        BufferedImage thumb = ImageIO.read(service.load(name.replace(".jpg", "_thumb.jpg")).getFile());
        assertThat(main.getWidth()).isEqualTo(1600);
        assertThat(main.getHeight()).isEqualTo(900);
        assertThat(thumb.getWidth()).isEqualTo(240);
        assertThat(thumb.getHeight()).isEqualTo(240);
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 거절하고, 규칙에 맞지 않는 파일 이름(경로 이동 등)은 찾지 않는다")
    void rejectsNonImageAndBadFileName() {
        ReviewImageService service = new ReviewImageService(tempDir.toString());

        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", "not an image".getBytes())))
                .isInstanceOfSatisfying(ReviewImageException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REVIEW_IMAGE));
        assertThatThrownBy(() -> service.load("../../etc/passwd"))
                .isInstanceOfSatisfying(ReviewImageException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REVIEW_IMAGE_NOT_FOUND));
        assertThat(ReviewImageService.isAllowedImageUrl("https://evil.example.com/a.jpg")).isFalse();
    }
}
