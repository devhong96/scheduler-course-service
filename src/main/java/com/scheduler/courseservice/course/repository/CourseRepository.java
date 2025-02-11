package com.scheduler.courseservice.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Supplier;

import static com.scheduler.courseservice.course.domain.QCourseSchedule.courseSchedule;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Repository
@RequiredArgsConstructor
public class CourseRepository {

    public final JPAQueryFactory queryFactory;

    @Lock(LockModeType.OPTIMISTIC)
    public Page<StudentCourseResponse> getStudentCourseList(
            String memberId, Pageable pageable
    ) {
        List<StudentCourseResponse> content = queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClass,
                        courseSchedule.tuesdayClass,
                        courseSchedule.wednesdayClass,
                        courseSchedule.thursdayClass,
                        courseSchedule.fridayClass))
                .from(courseSchedule)
                .where(
                        teacherIdEq(memberId),
                        studentIdEq(memberId)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> counts = queryFactory
                .select(courseSchedule.count())
                .from(courseSchedule)
                .where(
                        teacherIdEq(memberId),
                        studentIdEq(memberId)
                );

        return PageableExecutionUtils.getPage(content, pageable, counts::fetchOne);
    }

    public List<StudentCourseResponse> getStudentClass(){
        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClass,
                        courseSchedule.tuesdayClass,
                        courseSchedule.wednesdayClass,
                        courseSchedule.thursdayClass,
                        courseSchedule.fridayClass))
                .from(courseSchedule)
                .fetch();
    }

    public StudentCourseResponse getStudentClassByStudentId(String studentId){
        return queryFactory
                .select(
                        Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClass,
                        courseSchedule.tuesdayClass,
                        courseSchedule.wednesdayClass,
                        courseSchedule.thursdayClass,
                        courseSchedule.fridayClass
                ))
                .from(courseSchedule)
                .where(courseSchedule.studentId.eq(studentId))
                .fetchOne();
    }

    public List<StudentCourseResponse> getStudentClassByTeacherId(String teacherId){
        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClass,
                        courseSchedule.tuesdayClass,
                        courseSchedule.wednesdayClass,
                        courseSchedule.thursdayClass,
                        courseSchedule.fridayClass))
                .from(courseSchedule)
                .where(courseSchedule.teacherId.eq(teacherId))
                .fetch();
    }

    private BooleanBuilder teacherIdEq(String teacherId) {
        return nullSafeBooleanBuilder(() -> courseSchedule.teacherId.eq(teacherId));
    }

    private BooleanBuilder studentIdEq(String studentId) {
        return nullSafeBooleanBuilder(() -> courseSchedule.studentId.eq(studentId));
    }

    private BooleanBuilder nullSafeBooleanBuilder(Supplier<BooleanExpression> supplier) {
        try {
            BooleanExpression expression = supplier.get();
            return expression != null ? new BooleanBuilder(expression) : new BooleanBuilder();
        } catch (IllegalArgumentException | NullPointerException e) {
            return new BooleanBuilder();
        }
    }
}
