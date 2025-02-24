INSERT INTO teacher (teacher_id, name) VALUES
    ('teacher_001', 'Mr. Kim');

INSERT INTO student (student_id, student_name, student_username) VALUES
    ('student_001', 'Alice Kim', 'student_username_01'),
    ('student_002', 'Bob Lee', 'student_username_02'),
    ('student_003', 'Charlie Park', 'student_username_03'),
    ('student_004', 'David Choi', 'student_username_04'),
    ('student_005', 'Emma Jung', 'student_username_05'),
    ('student_006', 'Frank Moon', 'student_username_06'),
    ('student_007', 'Grace Han', 'student_username_07'),
    ('student_008', 'Henry Lim', 'student_username_08'),
    ('student_009', 'Irene Seo', 'student_username_09'),
    ('student_010', 'Jack Kang', 'student_username_10');

INSERT INTO course_schedule (
    teacher_id, student_id, student_name, course_year, week_of_year,
    monday_class_hour, tuesday_class_hour, wednesday_class_hour,
    thursday_class_hour, friday_class_hour, created_at, last_modified_at
)
VALUES
    ('teacher_001', 'student_001', 'Alice Kim', 2025, 1, 2, 3, 2, 4, 1, NOW(), NOW()),
    ('teacher_001', 'student_002', 'Bob Lee', 2025, 2, 1, 2, 2, 3, 2, NOW(), NOW()),
    ('teacher_001', 'student_003', 'Charlie Park', 2025, 3, 3, 1, 4, 2, 1, NOW(), NOW()),
    ('teacher_001', 'student_004', 'David Choi', 2025, 4, 2, 3, 1, 2, 3, NOW(), NOW()),
    ('teacher_001', 'student_005', 'Emma Jung', 2025, 5, 1, 2, 3, 2, 4, NOW(), NOW()),
    ('teacher_001', 'student_006', 'Frank Moon', 2025, 6, 3, 2, 2, 1, 3, NOW(), NOW()),
    ('teacher_001', 'student_007', 'Grace Han', 2025, 7, 2, 1, 3, 2, 4, NOW(), NOW()),
    ('teacher_001', 'student_008', 'Henry Lim', 2025, 8, 1, 3, 2, 2, 1, NOW(), NOW()),
    ('teacher_001', 'student_009', 'Irene Seo', 2025, 10, 3, 2, 1, 4, 2, NOW(), NOW()),
    ('teacher_001', 'student_010', 'Jack Kang', 2025, 10, 2, 3, 2, 1, 3, NOW(), NOW());