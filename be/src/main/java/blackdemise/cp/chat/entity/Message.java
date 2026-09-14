package blackdemise.cp.chat.entity;

import blackdemise.cp.chat.ChatIntent;
import blackdemise.cp.chat.MessageRole;
import blackdemise.cp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Populated only for ASSISTANT messages, from Gemini's usage metadata.
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    // Populated only for USER messages, from the intent classification call.
    @Enumerated(EnumType.STRING)
    private ChatIntent intent;
}
