package blackdemise.cp.interview;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import blackdemise.cp.interview.entity.InterviewTopic;

public interface InterviewTopicRepository extends JpaRepository<InterviewTopic, UUID> {
    List<InterviewTopic> findByActiveTrueOrderByLabelAsc();
}