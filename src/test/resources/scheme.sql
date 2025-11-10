DROP TABLE IF EXISTS course_schedule;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS student;

CREATE TABLE IF NOT EXISTS student
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id       VARCHAR(50)  NOT NULL UNIQUE,
    student_name     VARCHAR(255) NOT NULL,
    student_username VARCHAR(50)  NOT NULL
);


CREATE TABLE IF NOT EXISTS teacher
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS course_schedule
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id VARCHAR(50) NOT NULL,
    teacher_name VARCHAR(50),
    student_id VARCHAR(50) NOT NULL,
    student_name VARCHAR(50),
    course_year INT NOT NULL,
    week_of_year INT NOT NULL CHECK (week_of_year BETWEEN 1 AND 52),
    monday_class_hour INT CHECK (monday_class_hour BETWEEN 0 AND 10),
    tuesday_class_hour INT CHECK (tuesday_class_hour BETWEEN 0 AND 10),
    wednesday_class_hour INT CHECK (wednesday_class_hour BETWEEN 0 AND 10),
    thursday_class_hour INT CHECK (thursday_class_hour BETWEEN 0 AND 10),
    friday_class_hour INT CHECK (friday_class_hour BETWEEN 0 AND 10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id)

);


CREATE TABLE IF NOT EXISTS out_box
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency VARCHAR(255),
    event_type VARCHAR(255),
    payload TEXT,
    PRIMARY KEY (id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

