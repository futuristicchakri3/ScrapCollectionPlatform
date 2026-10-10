package com.scrapconnection.repository;

import com.scrapconnection.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long> {
    List<MaterialCategory> findByActiveTrue();
    Optional<MaterialCategory> findByCode(String code);
    boolean existsByCode(String code);
}
