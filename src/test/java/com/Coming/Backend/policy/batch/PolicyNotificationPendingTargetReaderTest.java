package com.Coming.Backend.policy.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.Coming.Backend.policy.entity.NotificationStatus;
import com.Coming.Backend.policy.entity.PolicyNotificationTarget;
import com.Coming.Backend.policy.repository.PolicyNotificationTargetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyNotificationPendingTargetReaderTest {

    @InjectMocks
    private PolicyNotificationPendingTargetReader policyNotificationPendingTargetReader;

    @Mock
    private PolicyNotificationTargetRepository policyNotificationTargetRepository;

    private static final Long POLICY_ID = 1L;

    private PolicyNotificationTarget buildTarget(Long id) {
        return PolicyNotificationTarget.builder()
                .id(id)
                .policyId(POLICY_ID)
                .userId(id)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    void should_return_items_in_order_when_first_batch_fetched_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationPendingTargetReader, "policyId", POLICY_ID);
        PolicyNotificationTarget target1 = buildTarget(1L);
        PolicyNotificationTarget target2 = buildTarget(2L);
        given(policyNotificationTargetRepository.findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 0L, PageRequest.of(0, 20)))
                .willReturn(List.of(target1, target2));

        // when
        PolicyNotificationTarget firstResult = policyNotificationPendingTargetReader.read();
        PolicyNotificationTarget secondResult = policyNotificationPendingTargetReader.read();

        // then
        assertThat(firstResult).isSameAs(target1);
        assertThat(secondResult).isSameAs(target2);
        verify(policyNotificationTargetRepository)
                .findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 0L, PageRequest.of(0, 20));
    }

    @Test
    void should_fetch_next_batch_using_last_read_id_as_cursor_when_current_batch_exhausted_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationPendingTargetReader, "policyId", POLICY_ID);
        List<PolicyNotificationTarget> firstBatch = List.of(buildTarget(19L), buildTarget(20L));
        PolicyNotificationTarget target21 = buildTarget(21L);
        given(policyNotificationTargetRepository.findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 0L, PageRequest.of(0, 20)))
                .willReturn(firstBatch);
        given(policyNotificationTargetRepository.findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 20L, PageRequest.of(0, 20)))
                .willReturn(List.of(target21));

        // when
        policyNotificationPendingTargetReader.read();
        policyNotificationPendingTargetReader.read();
        PolicyNotificationTarget resultFromNextBatch = policyNotificationPendingTargetReader.read();

        // then
        assertThat(resultFromNextBatch).isSameAs(target21);
        verify(policyNotificationTargetRepository)
                .findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 20L, PageRequest.of(0, 20));
    }

    @Test
    void should_return_null_when_repository_returns_empty_batch_given() {
        // given
        ReflectionTestUtils.setField(policyNotificationPendingTargetReader, "policyId", POLICY_ID);
        given(policyNotificationTargetRepository.findByPolicyIdAndStatusAndIdGreaterThanOrderByIdAsc(
                        POLICY_ID, NotificationStatus.PENDING, 0L, PageRequest.of(0, 20)))
                .willReturn(List.of());

        // when
        PolicyNotificationTarget result = policyNotificationPendingTargetReader.read();

        // then
        assertThat(result).isNull();
    }
}
