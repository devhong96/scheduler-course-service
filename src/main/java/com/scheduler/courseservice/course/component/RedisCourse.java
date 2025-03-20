package com.scheduler.courseservice.course.component;

import com.scheduler.courseservice.course.repository.CourseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.scheduler.courseservice.course.dto.CourseInfoResponse.StudentCourseResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCourse {

    private final CourseRepository courseRepository;
    private final RedissonClient redissonClient;

    private static final String TEACHER_CACHE_NAME = "teacherCourses";
    private static final String STUDENT_CACHE_NAME = "studentCourses";

    private static final long CACHE_TTL = 7;
    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    @PostConstruct
    @Scheduled(cron = "30 59 23 * * SUN", zone = "Asia/Seoul")
    public void preloadAllDataToRedis() {
        RLock lock = redissonClient.getLock("redis_course_lock");

        if (!lock.tryLock()) {
            log.info("이미 다른 인스턴스에서 실행 중이므로 종료.");
            return;
        }

        try {
            log.info("🔄 전체 데이터 Redis에 미리 캐싱");
            redisTemplate.delete(redisTemplate.keys(TEACHER_CACHE_NAME + "*"));
            redisTemplate.delete(redisTemplate.keys(STUDENT_CACHE_NAME + "*"));

            List<StudentCourseResponse> allCourses = courseRepository.findAllSchedule();
            if (allCourses.isEmpty()) {
                log.info("⚠ 데이터 없음. 캐싱하지 않음.");
                return;
            }

            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (StudentCourseResponse course : allCourses) {
                    String cacheKey = generateTeacherCacheKey(course);
                    redisTemplate.opsForValue().set(cacheKey, course, CACHE_TTL, TimeUnit.DAYS);
                }
                return null;
            });

            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (StudentCourseResponse course : allCourses) {
                    String cacheKey = generateStudentCacheKey(course);
                    redisTemplate.opsForValue().set(cacheKey, course, CACHE_TTL, TimeUnit.DAYS);
                }
                return null;
            });

        } finally {
            lock.unlock(); // 락 해제
        }
    }

    private String generateTeacherCacheKey(StudentCourseResponse course) {
        return TEACHER_CACHE_NAME + ":teacherId:" + course.getTeacherId()
                + ":year:" + course.getCourseYear()
                + ":weekOfYear:" + course.getWeekOfYear();
    }

    private String generateStudentCacheKey(StudentCourseResponse course) {
        return STUDENT_CACHE_NAME + ":studentId:" + course.getStudentId()
                + ":year:" + course.getCourseYear()
                + ":weekOfYear:" + course.getWeekOfYear();
    }
}
