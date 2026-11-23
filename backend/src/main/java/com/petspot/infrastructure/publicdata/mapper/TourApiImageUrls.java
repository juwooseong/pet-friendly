package com.petspot.infrastructure.publicdata.mapper;

/**
 * TourAPI 이미지 주소 정리 (TSK-DATA-013).
 * <p>일부 장소(58곳, 2026-11-23)의 사진 주소가 {@code http://tong.visitkorea.or.kr}로 내려온다. HTTPS 사이트에서는
 * 브라우저가 http 이미지를 막거나 경고하므로, 같은 경로가 https로도 열리는 관광공사 사진 서버 주소는 https로 바꾼다
 * (실제 확인: http 주소 5건 모두 https로 정상 응답). 다른 서버 주소는 그대로 둔다.
 */
public final class TourApiImageUrls {

    private static final String TOUR_IMAGE_HTTP = "http://tong.visitkorea.or.kr/";
    private static final String TOUR_IMAGE_HTTPS = "https://tong.visitkorea.or.kr/";

    private TourApiImageUrls() {
    }

    /** http(s) 주소만 받고(그 외·빈 값은 null), 관광공사 사진 서버의 http 주소는 https로 바꾼다 */
    public static String normalize(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith(TOUR_IMAGE_HTTP)) {
            return TOUR_IMAGE_HTTPS + trimmed.substring(TOUR_IMAGE_HTTP.length());
        }
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : null;
    }
}
