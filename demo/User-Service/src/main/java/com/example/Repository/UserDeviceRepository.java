package com.example.Repository;

import com.example.Model.Entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceIdAndIsActiveTrue(Long userId, String deviceId);

    List<UserDevice> findAllByUserIdAndIsActiveTrue(Long userId);
}