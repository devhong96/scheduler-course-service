package com.scheduler.courseservice.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scheduler.courseservice.course.component.DateProvider;
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

    private final DateProvider dateProvider;
    private final JPAQueryFactory queryFactory;

    @Lock(LockModeType.OPTIMISTIC)
    public Page<StudentCourseResponse> findAllStudentsCourses(
            String memberId, Pageable pageable
    ) {
        List<StudentCourseResponse> contents = queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour))
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

        return PageableExecutionUtils.getPage(contents, pageable, counts::fetchOne);
    }

    public List<StudentCourseResponse> getWeeklyCoursesForAllStudents(){

        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour))
                .from(courseSchedule)
                .where(
                        yearEq(currentYear),
                        weekNumberEq(currentWeek))
                .fetch();
    }

    public StudentCourseResponse getWeeklyClassByStudentId(String studentId){

        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        return queryFactory
                .select(
                        Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour
                ))
                .from(courseSchedule)
                .where(
                        studentIdEq(studentId),
                        yearEq(currentYear),
                        weekNumberEq(currentWeek)
                )
                .fetchOne();
    }

    private BooleanBuilder yearEq(int year) {
        return nullSafeBooleanBuilder(() -> courseSchedule.year.eq(year));
    }

    private BooleanBuilder weekNumberEq(int weekNumber) {
        return nullSafeBooleanBuilder(() -> courseSchedule.weekOfYear.eq(weekNumber));
    }

    public List<StudentCourseResponse> getStudentClassByTeacherId(String teacherId){
        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour))
                .from(courseSchedule)
                .where(teacherIdEq(teacherId))
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
