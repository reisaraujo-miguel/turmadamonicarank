package rocks.leug.turmadamonicarank.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import rocks.leug.turmadamonicarank.user.User;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHash(byte[] tokenHash);

    List<AuthToken> findByUserIdAndPurposeAndConsumedAtIsNull(
            UUID userId, AuthTokenPurpose purpose);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AuthToken t where t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);

    default List<AuthToken> findOutstanding(User user, AuthTokenPurpose purpose) {
        return findByUserIdAndPurposeAndConsumedAtIsNull(user.getId(), purpose);
    }
}
