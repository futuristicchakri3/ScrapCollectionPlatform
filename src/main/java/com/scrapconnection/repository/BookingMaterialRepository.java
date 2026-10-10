package com.scrapconnection.repository;

import com.scrapconnection.entity.BookingMaterial;
import com.scrapconnection.entity.ScrapBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingMaterialRepository extends JpaRepository<BookingMaterial, Long> {
    List<BookingMaterial> findByBooking(ScrapBooking booking);
}
