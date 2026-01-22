package com.example.scoi.domain.transfer.repository;

import com.example.scoi.domain.transfer.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    Boolean existsByMemberIdAndWalletAddress(Long memberId, String walletAddress);

    @Query("SELECT r FROM Recipient r WHERE r.member.id = :memberId " +
            "AND r.isFavorite = true " +
            "AND (:lastId IS NULL OR r.id > :lastId) " +
            "ORDER BY r.id ASC")
    Slice<Recipient> findByMemberIdAndIsFavoriteTrue(
            @Param("memberId") Long memberId,
            @Param("lastId") Long lastId,
            Pageable pageable);


}
