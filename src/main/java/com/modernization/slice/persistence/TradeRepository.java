package com.modernization.slice.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, String> {
    /** All reads are tenant-scoped: no repository method can cross tenants by accident. */
    Optional<TradeEntity> findByIdAndTenantId(String id, String tenantId);
    List<TradeEntity> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
