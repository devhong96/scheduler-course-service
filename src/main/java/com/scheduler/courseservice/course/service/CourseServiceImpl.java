package com.scheduler.courseservice.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scheduler.courseservice.client.MemberServiceClient;
import com.scheduler.courseservice.course.domain.CourseSchedule;
import com.scheduler.courseservice.course.repository.CourseJpaRepository;
import com.scheduler.courseservice.infra.exception.custom.DuplicateCourseException;
import com.scheduler.courseservice.outbox.service.CourseCreatedEventPayload;
import com.scheduler.courseservice.outbox.service.OutBoxEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.StudentInfo;
import static com.scheduler.courseservice.course.dto.CourseInfoRequest.UpsertCourseRequest;
import static com.scheduler.courseservice.course.messaging.RabbitMQDto.ChangeMemberNameDto;
import static com.scheduler.courseservice.outbox.domain.EventType.CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final MemberServiceClient memberServiceClient;
    private final CourseJpaRepository courseJpaRepository;
    private final OutBoxEventPublisher outBoxEventPublisher;
    private final CourseMessageService courseMessageService;

    @Override
    @Transactional("transactionManager")
    @CircuitBreaker(name = "studentService", fallbackMethod = "fallbackSaveClassTable")
    public void applyCourse(String token, UpsertCourseRequest upsertCourseRequest) {

        StudentInfo studentInfo = memberServiceClient.findStudentInfoByToken(token);
        outBoxEventPublisher.publish(
                CREATED, new CourseCreatedEventPayload(studentInfo, upsertCourseRequest)
        );
    }

    protected void fallbackSaveClassTable(
            String token, UpsertCourseRequest upsertCourseRequest, Throwable e
    ) {
        log.warn("Reason: ", e);
        throw new RuntimeException("수업 정보를 저장할 수 없습니다. 다시 시도해 주세요.");
    }

    @KafkaListener(
            topics = "${spring.kafka.topics.course.apply}",
            groupId = "${spring.kafka.topics.course.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void saveCourseTable(
            @Header(name = "Idempotency-Key", required = false) List<String> idemKeys,
            @Payload List<String> messages,
            Acknowledgment ack
    ) {
        if (messages.isEmpty()){
            ack.acknowledge();
            return;
        }

        try {
            for (int i = 0; i < messages.size(); i++) {
                String idem = (idemKeys != null && idemKeys.size() > i) ? idemKeys.get(i) : null;
                String message = messages.get(i);

                try {
                    courseMessageService.processMessage(idem, message);
                } catch (DuplicateCourseException e) {
                    log.warn("중복 메시지 처리 건너뜀 (배치 계속 진행): message = {}, reason = {}", message, e.getMessage());
                } catch (JsonProcessingException e) {
                    log.warn("JsonType Error : message = {}, errorMessage = {}", message, e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("배치 처리 중 재시도 필요한 오류 발생 :", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    @Transactional
    public void changeStudentName(ChangeMemberNameDto changeMemberNameDto) {

        String studentId = changeMemberNameDto.getMemberId();

        CourseSchedule courseSchedule = courseJpaRepository.findCourseScheduleByStudentId(studentId)
                .orElseThrow(NoSuchElementException::new);

        courseSchedule.updateStudentName(changeMemberNameDto.getNewName());
    }

}