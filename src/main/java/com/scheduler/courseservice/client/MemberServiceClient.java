package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;

@FeignClient(name = "member-service", configuration = MemberFeignErrorDecoder.class)
public interface MemberServiceClient {

    @PostMapping("{studentName}/{password}")
    StudentInfo findCourseByStudentNameAndPassword(@PathVariable String studentName, @PathVariable String password);

    @GetMapping("{studentId}")
    TeacherInfo findTeacherByStudentId(@PathVariable String studentId);

    @GetMapping("memberInfo")
    MemberInfo findMemberInfoByToken(@RequestHeader("Authorization") String token);
}