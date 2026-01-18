package com.example.scoi.domain.transfer.repository;

import com.example.scoi.domain.transfer.entity.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
}
