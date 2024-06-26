package com.example.app.repository;

import com.example.app.dto.UserDetailsDTO;
import com.example.app.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    @Query("SELECT new com.example.app.dto.UserDetailsDTO(u2.userSystemId, u2.userName, u1.ipAddress, u1.customPath, u1.customPathEnableFlag, u1.pathType, u1.optionUsername, u1.optionPassword) " +
           "FROM User u1 " +
           "JOIN UserMaster u2 ON u1.userId = u2.userId " +
           "WHERE u2.userSystemId = :userSystemId")
    List<UserDetailsDTO> findUserDetailsByUserSystemId(@Param("userSystemId") String userSystemId);
}
