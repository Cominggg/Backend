package com.Coming.Backend.common.util;

import java.util.Locale;

/**
 * LIKE 검색어의 와일드카드를 이스케이프한다. 쿼리 쪽에 반드시 {@code ESCAPE '\'}를 함께 써야 한다.
 * (Hibernate는 ESCAPE 절이 없는 LIKE에 {@code escape ''}를 붙여 이스케이프를 무효화한다)
 */
public final class LikeEscapeUtils {

    private LikeEscapeUtils() {}

    /**
     * {@code \}·{@code %}·{@code _} 앞에 {@code \}를 붙인다.
     */
    public static String escape(String keyword) {
        return keyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * 소문자로 바꾸고 이스케이프한 뒤 앞뒤에 {@code %}를 붙인 부분 일치 패턴을 반환한다.
     */
    public static String containsPattern(String keyword) {
        return "%" + escape(keyword.toLowerCase(Locale.ROOT)) + "%";
    }
}
