package com.TravelShare.repository;

import com.TravelShare.entity.Request;
import com.TravelShare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;


public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {
    List<Request> findByReceiver(User receiver);
    List<Request> findBySender(User sender);
    List<Request> findByReceiverAndStatus(User receiver, String status);
    Optional<Request> findByReferenceIdAndType(Long referenceId, String type);
    List<Request> findAllByReferenceIdAndType(Long referenceId, String type);
}
