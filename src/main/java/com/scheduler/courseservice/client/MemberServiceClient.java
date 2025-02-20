package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;

@FeignClient(
        name = "scheduler-member-service",
        path = "/scheduler-member-service/feign-member/",
        configuration = MemberFeignErrorDecoder.class
)
public interface MemberServiceClient {

    @PostMapping("student/{username}")
    StudentInfo findStudentInfoByToken(
            @RequestHeader("Authorization") String token
    );

    @GetMapping("memberInfo")
    MemberInfo findMemberInfoByToken(
            @RequestHeader("Authorization") String token
    );

    TeacherInfo findTeachersClasses(String token);
}