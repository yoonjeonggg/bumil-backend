package com.example.bumil_backend.repository;

import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Tag;
import com.example.bumil_backend.entity.Users;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT c FROM ChatRoom c WHERE :tag IS NULL OR c.tag = :tag")
    List<ChatRoom> findByTag(@Param("tag") Tag tag, Sort sort);

    @Query("SELECT c FROM ChatRoom c WHERE (:tag IS NULL OR c.tag = :tag) AND c.author = :author")
    List<ChatRoom> findByTagAndAuthor(
            @Param("tag") Tag tag,
            @Param("author") Users author,
            Sort sort
    );
}
