package com.scheduler.courseservice.client;

import com.scheduler.courseservice.infra.error.MemberFeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;

import static com.scheduler.courseservice.client.dto.FeignMemberInfo.*;

@FeignClient(name = "member-service", configuration = MemberFeignErrorDecoder.class)
public interface MemberServiceClient {

    StudentInfo findCourseByStudentName(String studentName, String password);

    TeacherInfo findTeacherByStudentId(String studentId);

    MemberInfo findMemberInfoByToken(String token);

    StudentInfo findStudentByStudentNameAndPassword(String studentName, String password);
}