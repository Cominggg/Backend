package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationTargetWriterTest {

    @InjectMocks
    private PolicyNotificationTargetWriter policyNotificationTargetWriter;

    @Mock
    private PolicyNotificationTargetRepository policyNotificationTargetRepository;

    private static final Long POLICY_ID = 1L;

    private PolicyNotificationTarget buildTarget(Long userId, NotificationStatus status) {
        return PolicyNotificationTarget.builder()
                .id(userId)
                .policyId(POLICY_ID)
                .userId(userId)
                .status(status)
                .retryCount(0)
                .build();
    }

    @Test
    void should_save_all_chunk_items_when_write_called_given() {
        // given
        PolicyNotificationTarget sentTarget = buildTarget(1L, NotificationStatus.SENT);
        PolicyNotificationTarget failedTarget = buildTarget(2L, NotificationStatus.FAILED);
        Chunk<PolicyNotificationTarget> chunk = new Chunk<>(List.of(sentTarget, failedTarget));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PolicyNotificationTarget>> captor = ArgumentCaptor.forClass(List.class);

        // when
        policyNotificationTargetWriter.write(chunk);

        // then
        verify(policyNotificationTargetRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(sentTarget, failedTarget);
    }
}
