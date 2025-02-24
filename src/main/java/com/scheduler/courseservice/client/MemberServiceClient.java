package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.scheduler.courseservice.client.request.dto.FeignMemberInfo.*;

@FeignClient(
        name = "scheduler-member-service",
        url =  "${member_service_url:}",
        path = "/feign-member",
        configuration = MemberFeignErrorDecoder.class
)
public interface MemberServiceClient {

    @GetMapping("teacher/class")
    TeacherInfo findTeachersClasses(
            @RequestHeader("Authorization") String token
    );

    @PostMapping("student/{username}")
    StudentInfo findStudentInfoByToken(
            @RequestHeader("Authorization") String token
    );

    @GetMapping("member/info")
    MemberInfo findMemberInfoByToken(
            @RequestHeader("Authorization") String token
    );
}