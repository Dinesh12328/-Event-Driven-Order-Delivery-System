package com.dinesh.orderdelivery.restaurant.repository;

import com.dinesh.orderdelivery.restaurant.domain.Restaurant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    @Query("""
            select r from Restaurant r
            where r.active = true
              and (:query is null or lower(r.name) like lower(concat('%', :query, '%')) or lower(r.cuisine) like lower(concat('%', :query, '%')))
              and (:cuisine is null or lower(r.cuisine) = lower(:cuisine))
              and (:location is null or lower(r.location) like lower(concat('%', :location, '%')))
            order by r.name
            """)
    List<Restaurant> search(
            @Param("query") String query,
            @Param("cuisine") String cuisine,
            @Param("location") String location
    );
}

