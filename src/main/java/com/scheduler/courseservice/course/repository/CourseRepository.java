package com.scheduler.courseservice.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Supplier;

import static com.scheduler.courseservice.course.domain.QCourseSchedule.courseSchedule;
import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;
import static org.springframework.data.support.PageableExecutionUtils.getPage;

@Repository
@RequiredArgsConstructor
public class CourseRepository {

    private final JPAQueryFactory queryFactory;

    @Cacheable(
            cacheNames = "findAllStudentsCourses",
            key = "'studentsCourses:page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize",
            cacheManager = "courseCacheManager"
    )
    public Page<StudentCourseResponse> findAllStudentsCourses(Pageable pageable) {
        List<StudentCourseResponse> contents = queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.studentId,
                        courseSchedule.studentName,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour,
                        courseSchedule.courseYear,
                        courseSchedule.weekOfYear
                ))
                .from(courseSchedule)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> totalCount = queryFactory
                .select(courseSchedule.count())
                .from(courseSchedule);

        return getPage(contents, pageable, totalCount::fetchOne);
    }

    public List<StudentCourseResponse> getTeacherWeeklyCoursesForComparison(
            String teacherId, Integer year, Integer weekOfYear) {

        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.studentId,
                        courseSchedule.studentName,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour,
                        courseSchedule.courseYear,
                        courseSchedule.weekOfYear
                ))
                .from(courseSchedule)
                .where(
                        yearEq(year),
                        weekOfYearEq(weekOfYear),
                        teacherIdEq(teacherId)
                )
                .fetch();
    }

    public List<StudentCourseResponse> getAllStudentsWeeklyCoursesForComparison(
            Integer year, Integer weekOfYear
    ){
        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.studentId,
                        courseSchedule.studentName,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour,
                        courseSchedule.courseYear,
                        courseSchedule.weekOfYear
                ))
                .from(courseSchedule)
                .where(
                        yearEq(year),
                        weekOfYearEq(weekOfYear)
                )
                .fetch();
    }

    public StudentCourseResponse getWeeklyCoursesByStudentId(
            String studentId, Integer year, Integer weekOfYear
    ) {
        StudentCourseResponse result = queryFactory
                .select(
                        Projections.fields(StudentCourseResponse.class,
                                courseSchedule.studentId,
                                courseSchedule.studentName,
                                courseSchedule.mondayClassHour,
                                courseSchedule.tuesdayClassHour,
                                courseSchedule.wednesdayClassHour,
                                courseSchedule.thursdayClassHour,
                                courseSchedule.fridayClassHour,
                                courseSchedule.courseYear,
                                courseSchedule.weekOfYear
                        ))
                .from(courseSchedule)
                .where(
                        studentIdEq(studentId),
                        yearEq(year),
                        weekOfYearEq(weekOfYear)
                )
                .fetchOne();

        return result != null ? result : new StudentCourseResponse();
    }

    public List<StudentCourseResponse> getStudentClassByTeacherId(
            String teacherId, int year, int weekOfYear
    ){
        return queryFactory
                .select(Projections.fields(StudentCourseResponse.class,
                        courseSchedule.studentId,
                        courseSchedule.studentName,
                        courseSchedule.mondayClassHour,
                        courseSchedule.tuesdayClassHour,
                        courseSchedule.wednesdayClassHour,
                        courseSchedule.thursdayClassHour,
                        courseSchedule.fridayClassHour,
                        courseSchedule.courseYear,
                        courseSchedule.weekOfYear
                ))
                .from(courseSchedule)
                .where(teacherIdEq(teacherId),
                        yearEq(year),
                        weekOfYearEq(weekOfYear)
                )
                .fetch();
    }

    private BooleanBuilder yearEq(int year) {
        return nullSafeBooleanBuilder(() -> courseSchedule.courseYear.eq(year));
    }

    private BooleanBuilder weekOfYearEq(int weekNumber) {
        return nullSafeBooleanBuilder(() -> courseSchedule.weekOfYear.eq(weekNumber));
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
