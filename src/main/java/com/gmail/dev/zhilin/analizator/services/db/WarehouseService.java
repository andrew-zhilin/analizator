package com.gmail.dev.zhilin.analizator.services.db;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gmail.dev.zhilin.analizator.entities.Warehouse;
import com.gmail.dev.zhilin.analizator.repositories.WarehouseRepository;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    
    @Autowired
    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }
    
    public Optional<Warehouse> findById(Long id) {
        return warehouseRepository.findById(id);
    }

    public Optional<Warehouse> findByName(String name) {
        return warehouseRepository.findByName(name);
    }
    
}
