package com.example.scoi.domain.myWallet.repository;

import com.example.scoi.domain.myWallet.entity.MyWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyWalletRepository extends JpaRepository<MyWallet,Long> {
}
