package com.scheduler.courseservice.outbox.repository;

import com.scheduler.courseservice.outbox.domain.OutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutBoxJpaRepository extends JpaRepository<OutBox, Long> {
}
