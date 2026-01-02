package com.example.scoi.domain.charge.repository;

import com.example.scoi.domain.charge.entity.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeRepository extends JpaRepository<Charge,Long> {
}
