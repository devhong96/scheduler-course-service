package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;

@FeignClient(
        name = "scheduler-member-service",
        path = "/scheduler-member-service/feign-member/",
        configuration = MemberFeignErrorDecoder.class
)
public interface MemberServiceClient {

    @PostMapping("{studentName}/{password}")
    StudentInfo findCourseByStudentNameAndPassword(@PathVariable String studentName, @PathVariable String password);

    @GetMapping("{studentId}")
    TeacherInfo findTeacherByStudentId(@PathVariable String studentId);

    @GetMapping("memberInfo")
    MemberInfo findMemberInfoByToken(@RequestHeader("Authorization") String token);

    @GetMapping("students/weekly-schedule")
    List<StudentInfo> findAllStudentsForWeeklySchedule();
}