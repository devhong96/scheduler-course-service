package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import io.swagger.v3.oas.annotations.Operation;
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


    @Operation(
            summary = "교사 정보 조회",
            description = "토큰을 이용하여 정보 조회"
    )
    @GetMapping("teacher/info")
    TeacherInfo findTeacherInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );

    @Operation(
            summary = "학생 수업 조회",
            description = "토큰을 이용하여 정보 조회"
    )
    @GetMapping("student/info")
    StudentInfo findStudentInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );

    @Operation(
            summary = "이용자의 아이디와 역할 조회"
    )
    @GetMapping("member/info")
    MemberInfo findMemberInfoByToken(
            @RequestHeader(AUTHORIZATION) String token
    );
}