package com.example.scoi.domain.transfer.repository;

import com.example.scoi.domain.transfer.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory,Long> {
}
