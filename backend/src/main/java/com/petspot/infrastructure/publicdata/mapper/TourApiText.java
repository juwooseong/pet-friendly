package com.petspot.infrastructure.publicdata.mapper;

/**
 * TourAPI 자유 텍스트 정리 유틸.
 *
 * <p>TourAPI의 개요/운영시간 필드는 화면용 HTML 조각({@code <br>}, {@code <a href>}, {@code &nbsp;} 등)이 섞여 온다.
 * 저장 전에 줄바꿈은 살리고 태그는 제거해 평문으로 만든다 - 프런트엔드는 이 값을 HTML이 아닌 텍스트로만 렌더링한다
 * (XSS 방지 - {@code v-html} 사용 금지).
 */
public final class TourApiText {

    private TourApiText() {
    }

    /**
     * HTML 조각을 평문으로 정리한다. 결과가 비어 있으면 null.
     */
    public static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ")   // 줄 안 공백 정리
                .replaceAll(" *\\n *", "\n")               // 줄 앞뒤 공백 제거
                .replaceAll("\\n{3,}", "\n\n")             // 빈 줄은 최대 1줄
                .trim();
        return text.isEmpty() ? null : text;
    }
}
