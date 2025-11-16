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
    teacher_id, teacher_name, student_id, student_name, course_year, week_of_year,
    monday_class_hour, tuesday_class_hour, wednesday_class_hour, thursday_class_hour, friday_class_hour,
    created_at, last_modified_at
)
VALUES
    ('teacher_001', 'Mr.Kim','student_001', 'Alice_Kim', YEAR(NOW()), 1,
     2, 3, 2, 4, 1,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_002', 'Bob_Lee', YEAR(NOW()), 2,
     1, 2, 2, 3, 2,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_003', 'Charlie_Park', YEAR(NOW()), 3,
     3, 1, 4, 2, 1,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_004', 'David_Choi', YEAR(NOW()), 4,
     2, 3, 1, 2, 3,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_005', 'Emma_Jung', YEAR(NOW()), 5,
     1, 2, 3, 2, 4,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_006', 'Frank_Moon', YEAR(NOW()), 6,
     3, 2, 2, 1, 3,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_007', 'Grace_Han', YEAR(NOW()), 7,
     2, 1, 3, 2, 4,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_008', 'Henry_Lim', YEAR(NOW()), 8,
     1, 3, 2, 2, 1,
     NOW(), NOW()),
    ('teacher_001', 'Mr.Kim','student_009', 'Irene_Seo', 2025, WEEK(CURDATE()),
     3, 2, 1, 4, 2,
     NOW(), NOW());
