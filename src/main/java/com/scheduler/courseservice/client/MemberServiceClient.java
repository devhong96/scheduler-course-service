package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(
        name = "scheduler-member-service",
        url =  "${scheduler_member_service_url:}",
        configuration = MemberFeignErrorDecoder.class
)
public interface MemberServiceClient {

    @PostMapping("/feign-member/teacher/info")
    TeacherInfo findTeachersClasses(
            @RequestHeader(AUTHORIZATION) String token
    );

    @PostMapping("/feign-member/student/info")
    StudentInfo findStudentInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );

    @PostMapping("/feign-member/member/info")
    MemberInfo findMemberInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );
}