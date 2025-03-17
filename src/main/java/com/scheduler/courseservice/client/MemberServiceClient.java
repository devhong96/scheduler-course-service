package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(
        name = "scheduler-member-service",
        path = "/feign-member",
        url =  "${scheduler_member_service_url:}",
        configuration = MemberFeignErrorDecoder.class
)
public interface MemberServiceClient {

    @GetMapping("teacher/info")
    TeacherInfo findTeacherInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );

    @GetMapping("student/info")
    StudentInfo findStudentInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );

    @GetMapping("member/info")
    MemberInfo findMemberInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );
}