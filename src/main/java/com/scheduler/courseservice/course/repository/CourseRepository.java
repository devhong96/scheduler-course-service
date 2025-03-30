package com.scheduler.courseservice.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.scheduler.courseservice.course.component.DateProvider;
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
    private final DateProvider dateProvider;

    public Page<StudentCourseResponse> findAllStudentsCourses(
            Pageable pageable, String keyword
    ) {
        List<StudentCourseResponse> contents = commonStudentCourse()
                .where(
                        studentNameContains(keyword)
                                .or(studentIdEq(keyword))
                                .or(teacherIdEq(keyword))
                                .or(teacherNameContains(keyword))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> totalCount = queryFactory
                .select(courseSchedule.count())
                .from(courseSchedule);

        return getPage(contents, pageable, totalCount::fetchOne);
    }

    @Cacheable(
            cacheNames = "teacherCourses",
            key = "'teacherCourses:teacherId:' + #teacherId + ':year:' + #year + ':weekOfYear:' + #weekOfYear",
            cacheManager = "courseCacheManager",
            condition =
                    "!(#year == T(java.time.LocalDate).now().getYear() && " +
                    "#weekOfYear == T(java.time.LocalDate).now().get(T(java.time.temporal.WeekFields).of(T(java.util.Locale).getDefault()).weekOfYear()))"
    )
    public List<StudentCourseResponse> getWeeklyCoursesByTeacherId(
            String teacherId, Integer year, Integer weekOfYear
    ){
        return commonStudentCourse()
                .where(
                        yearEq(year),
                        weekOfYearEq(weekOfYear),
                        teacherIdEq(teacherId)
                )
                .fetch();
    }

    @Cacheable(
            cacheNames = "studentCourses",
            key = "'studentCourses:studentId:' + #studentId + ':year:' + #year + ':weekOfYear:' + #weekOfYear",
            cacheManager = "courseCacheManager",
            condition =
                    "!(#year == T(java.time.LocalDate).now().getYear() && " +
                    "#weekOfYear == T(java.time.LocalDate).now().get(T(java.time.temporal.WeekFields).of(T(java.util.Locale).getDefault()).weekOfYear()))"
    )
    public StudentCourseResponse getWeeklyCoursesByStudentId(
            String studentId, Integer year, Integer weekOfYear
    ) {
        StudentCourseResponse result = commonStudentCourse()
                .where(
                        yearEq(year),
                        weekOfYearEq(weekOfYear),
                        studentIdEq(studentId)
                )
                .fetchOne();

        return result != null ? result : new StudentCourseResponse();
    }

    public List<StudentCourseResponse> findAllSchedule() {

        int currentYear = dateProvider.getCurrentYear();
        int currentWeek = dateProvider.getCurrentWeek();

        return commonStudentCourse()
                .where(
                        courseSchedule.courseYear.eq(currentYear)
                                .and(courseSchedule.weekOfYear.eq(currentWeek))
                                .not()
                )
                .fetch();
    }

    public JPAQuery<StudentCourseResponse> commonStudentCourse() {
        return queryFactory
                .select(
                        Projections.fields(StudentCourseResponse.class,
                                courseSchedule.studentId,
                                courseSchedule.studentName,
                                courseSchedule.teacherId,
                                courseSchedule.teacherName,
                                courseSchedule.mondayClassHour,
                                courseSchedule.tuesdayClassHour,
                                courseSchedule.wednesdayClassHour,
                                courseSchedule.thursdayClassHour,
                                courseSchedule.fridayClassHour,
                                courseSchedule.courseYear,
                                courseSchedule.weekOfYear
                        ))
                .from(courseSchedule);
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

    private BooleanBuilder teacherNameContains(String teacherName) {
        return nullSafeBooleanBuilder(() -> courseSchedule.teacherName.contains(teacherName));
    }

    private BooleanBuilder studentIdEq(String studentId) {
        return nullSafeBooleanBuilder(() -> courseSchedule.studentId.eq(studentId));
    }

    private BooleanBuilder studentNameContains(String studentName) {
        return nullSafeBooleanBuilder(() -> courseSchedule.studentName.contains(studentName));
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
