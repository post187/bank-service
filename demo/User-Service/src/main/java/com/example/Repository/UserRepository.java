package com.example.Repository;

import com.example.Model.Entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository {
    @Query("select * from users u where u.authId := authId")
    Optional<User> findUserByAuthId(String authId);
}
