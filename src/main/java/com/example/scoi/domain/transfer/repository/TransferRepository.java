package com.example.scoi.domain.transfer.repository;

import com.example.scoi.domain.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer,Long> {
}
