package com.Coming.Backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserRole;
import com.Coming.Backend.auth.entity.UserStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static final List<UserStatus> NOT_WITHDRAWN = List.of(UserStatus.ACTIVE, UserStatus.SUSPENDED);

    private User saveUser(String providerId, String email, String nickname, UserStatus status) {
        return userRepository.save(User.builder()
                .provider("google")
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .role(UserRole.USER)
                .status(status)
                .build());
    }

    @Test
    void should_find_users_by_email_or_nickname_ignoring_case_when_search() {
        // given
        User byEmail = saveUser("repo-test-1", "ZQ139.fan@example.com", "가수팬", UserStatus.ACTIVE);
        User byNickname = saveUser("repo-test-2", "other@example.com", "zq139닉네임", UserStatus.SUSPENDED);
        saveUser("repo-test-3", "none@example.com", "무관한회원", UserStatus.ACTIVE);

        // when
        Page<User> result = userRepository.searchByEmailOrNickname("zq139", NOT_WITHDRAWN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(User::getId)
                .containsExactlyInAnyOrder(byEmail.getId(), byNickname.getId());
    }

    @Test
    void should_exclude_withdrawn_users_when_search() {
        // given
        saveUser("repo-test-4", "zq139.withdrawn@example.com", "zq139탈퇴", UserStatus.INACTIVE);

        // when
        Page<User> result = userRepository.searchByEmailOrNickname("zq139", NOT_WITHDRAWN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void should_find_user_with_null_email_by_nickname_when_search() {
        // given
        User user = saveUser("repo-test-5", null, "zq139애플", UserStatus.ACTIVE);

        // when
        Page<User> result = userRepository.searchByEmailOrNickname("ZQ139", NOT_WITHDRAWN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(User::getId).containsExactly(user.getId());
    }

    @Test
    void should_match_wildcard_literally_when_keyword_is_escaped() {
        // given
        User literal = saveUser("repo-test-6", "zq139_fan@example.com", "언더바회원", UserStatus.ACTIVE);
        saveUser("repo-test-7", "zq139xfan@example.com", "다른회원", UserStatus.ACTIVE);

        // when
        Page<User> result = userRepository.searchByEmailOrNickname("zq139\\_fan", NOT_WITHDRAWN, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(User::getId).containsExactly(literal.getId());
    }
}
