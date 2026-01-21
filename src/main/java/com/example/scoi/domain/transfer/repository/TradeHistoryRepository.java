package com.example.scoi.domain.transfer.repository;

import com.example.scoi.domain.transfer.entity.TradeHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory,Long> {

    @Query("SELECT th FROM TradeHistory th " +
            "JOIN FETCH th.recipient r " +
            "WHERE th.member.id = :memberId " +
            "AND (:onlyFavorite = false OR r.isFavorite = true)" + // false인 경우 항상 참, true인 경우는 isFavorite 값이 참인 경우만 가져옴
            "AND th.id IN (" +
            "    SELECT MAX(th2.id) FROM TradeHistory th2 " +
            "    WHERE th2.member.id = :memberId " +
            "    GROUP BY th2.recipient" +
            ") " +
            "AND (:lastTime IS NULL OR (th.createdAt < :lastTime) " +
            "     OR (th.createdAt = :lastTime AND th.id < :lastId)) " +
            "ORDER BY th.createdAt DESC, th.id DESC")
    Slice<TradeHistory> findRecentUniqueRecipients(
            @Param("memberId") Long memberId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") Long lastId,
            @Param("onlyFavorite") boolean onlyFavorite,
            Pageable pageable
    );
}
