package blackdemise.cp.chat.entity;

import blackdemise.cp.common.BaseEntity;
import blackdemise.cp.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    // Rolling summary of messages older than the recent window kept in every prompt (see
    // ChatService.buildConversationPrompt); null until the conversation is long enough to need one.
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private int summarizedThroughCount;
}
