package com.TravelShare.repository;

import com.TravelShare.entity.Request;
import com.TravelShare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByReceiver(User receiver);
    List<Request> findBySender(User sender);
    List<Request> findByReceiverAndStatus(User receiver, String status);
    Optional<Request> findByReferenceIdAndType(Long referenceId, String type);
}
