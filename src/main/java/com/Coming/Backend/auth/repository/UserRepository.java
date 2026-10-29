package com.Coming.Backend.auth.repository;

import com.Coming.Backend.auth.entity.User;
import com.Coming.Backend.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    List<User> findAllByIdIn(Collection<Long> ids);

    boolean existsByNickname(String nickname);

    List<User> findByStatusIn(Collection<UserStatus> statuses);

    Page<User> findByStatusIn(Collection<UserStatus> statuses, Pageable pageable);

    /**
     * 이메일 또는 닉네임에 keyword가 포함된 회원을 대소문자 구분 없이 조회한다.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.status IN :statuses
              AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<User> searchByEmailOrNickname(@Param("keyword") String keyword,
                                       @Param("statuses") Collection<UserStatus> statuses,
                                       Pageable pageable);
}
