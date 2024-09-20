package com.gmail.dev.zhilin.analizator.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.gmail.dev.zhilin.analizator.entities.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    Optional<Item> findById(Long id);
    
    Optional<Item> findByCode(String code);
    
    List<Item> findAll();

    Page<Item> findAll(Pageable pageable);

    List<Item> findByOrderByCreatedAtDesc(Limit limit);
    
    List<Item> findByPartNumberContainsIgnoreCaseOrCrossReferenceContainsIgnoreCase(String snippet, String snippet2);
    
}
