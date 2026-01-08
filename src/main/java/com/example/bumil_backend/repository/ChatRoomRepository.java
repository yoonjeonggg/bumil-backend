package com.example.bumil_backend.repository;

import com.example.bumil_backend.entity.ChatRoom;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c WHERE :tag IS NULL OR c.tag = :tag")
    List<ChatRoom> findByTag(@Param("tag") String tag, Sort sort);
}
