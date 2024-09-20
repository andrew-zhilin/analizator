package com.gmail.dev.zhilin.analizator.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    public Optional<Warehouse> findById(Long id);
    
    public Optional<Warehouse> findByName(String name);
    
}
