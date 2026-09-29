package dev.hyuki.investment_openapi.account.repository;

import dev.hyuki.investment_openapi.account.entity.AccountRestriction;
import dev.hyuki.investment_openapi.account.entity.RestrictionType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRestrictionRepository extends JpaRepository<AccountRestriction, UUID> {

  @Query("""
      SELECT CASE WHEN COUNT(restriction) > 0 THEN TRUE ELSE FALSE END
      FROM AccountRestriction restriction
      WHERE restriction.accountId = :accountId
        AND restriction.restrictionType IN :types
        AND restriction.effectiveFrom <= :evaluatedAt
        AND (restriction.effectiveUntil IS NULL OR restriction.effectiveUntil > :evaluatedAt)
        AND (restriction.releasedAt IS NULL OR restriction.releasedAt > :evaluatedAt)
      """)
  boolean existsActiveRestriction(
      @Param("accountId") UUID accountId,
      @Param("types") Set<RestrictionType> types,
      @Param("evaluatedAt") Instant evaluatedAt
  );
}
