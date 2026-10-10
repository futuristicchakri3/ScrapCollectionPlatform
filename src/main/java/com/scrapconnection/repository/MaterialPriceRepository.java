package com.scrapconnection.repository;

import com.scrapconnection.entity.MaterialCategory;
import com.scrapconnection.entity.MaterialPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialPriceRepository extends JpaRepository<MaterialPrice, Long> {
    Optional<MaterialPrice> findByCategoryAndActiveTrue(MaterialCategory category);
    Optional<MaterialPrice> findByCategoryCodeAndActiveTrue(String categoryCode);
    List<MaterialPrice> findByActiveTrue();
    List<MaterialPrice> findByCategory(MaterialCategory category);
}
