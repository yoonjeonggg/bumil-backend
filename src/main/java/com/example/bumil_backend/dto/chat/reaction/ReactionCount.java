package com.example.bumil_backend.dto.chat.reaction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReactionCount {
    private int likeCnt;
    private int dislikeCnt;
}
