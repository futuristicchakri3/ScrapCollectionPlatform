package com.scrapconnection.repository;

import com.scrapconnection.entity.Address;
import com.scrapconnection.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerProfile(CustomerProfile customerProfile);
    List<Address> findByCustomerProfileId(Long customerProfileId);
}
