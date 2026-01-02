package com.example.scoi.domain.invest.repository;

import com.example.scoi.domain.invest.entity.Invest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestRepository extends JpaRepository<Invest,Long> {
}
