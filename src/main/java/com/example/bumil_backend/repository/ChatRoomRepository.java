package com.example.bumil_backend.repository;

import com.example.bumil_backend.entity.ChatRoom;
import com.example.bumil_backend.entity.Users;
import com.example.bumil_backend.enums.ChatTags;
import com.example.bumil_backend.enums.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 모든 채팅 조회 + 태그 필터
    Page<ChatRoom> findAllByTagAndIsDeletedFalse(
            ChatTags chatTag,
            Pageable pageable
    );

    // 삭제되지 않은 채팅방 조회
    Optional<ChatRoom> findByIdAndIsDeletedFalse(Long id);

    // 모든 채팅 조회
    Page<ChatRoom> findAllByIsDeletedFalse(Pageable pageable);


    @Query("""
            SELECT c FROM ChatRoom c
            WHERE c.isDeleted = false
            AND (:tag IS NULL OR c.tag = :tag)
           """)
    List<ChatRoom> findAllByTagAndIsDeletedFalse(@Param("tag") Tag tag, Sort sort);

    @Query("""
            SELECT c FROM ChatRoom c
            WHERE c.isDeleted = false
            AND (:tag IS NULL OR c.tag = :tag)
            AND c.author = :author
           """)
    List<ChatRoom> findByTagAndAuthorAndIsDeletedFalse(
            @Param("tag") Tag tag,
            @Param("author") Users author,
            Sort sort
    );

    @Query("""
        select c
        from ChatRoom c
        where c.isPublic = true
          and c.isDeleted = false
          and lower(c.title) like lower(concat('%', :keyword, '%'))
        order by c.createdAt desc
    """)
    List<ChatRoom> searchPublicChatsByTitle(@Param("keyword") String keyword);


    Optional<ChatRoom> findByIdAndIsDeletedFalseAndIsPublicTrue(Long chatRoomId);
}


