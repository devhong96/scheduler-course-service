# 📌Scheduler-Course-Service

---
Scheduler-Course-Service는 일주일 기준 학생과 교사의 수업 스케줄 관리 기능을 제공합니다. 

이 서비스는 다양한 사용자(학생, 교사)들이 자신의 수업 일정을 효율적으로 확인하고 관리할 수 있도록 API를 제공합니다.

---
## 🛠 API

🔗 [Swagger UI](https://seho0218.synology.me:8087/swagger-ui/index.html?urls.primaryName=scheduler-course-service)

---

## 🛠 설계 및 구조

멀티 컨테이너 환경에서 서비스되며 API Gateway에서 1차적으로 토큰을 이용해 보안성을 검증하고 각 서비스간통신은 openfeign을 통해 이루어 집니다. 

<br>

현재 약 160만개의 데이터를 저장하고 있으며 학생 1만명, 교사 2001명으로 이루어져있습니다.

 주어진 시간에 1명의 교사당 1명의 학생만 수업을 들을 수 있는 1:1수업 방식이며 동시에 2명이상 수업을 들을 수 없습니다.

<br>

 일주일 단위로 수강 신청을 진행할 수 있으며 해당 주차가 지날 경우, 변경이 불가하며 자동으로 Redis에 저장하여 조회 속도를 최적화 하였으며 Mysql에도 저장되는 구조입니다.


 Redis와 Redisson을 이용하여 내부적으로 studentId + 시간 기준으로 중복 처리를 방지했고, Kafka 메시지가 중복 소비되더라도 DB에서 이미 처리된 데이터는 업데이트만 하도록 설계되어있고 Redisson Lock도 같이 사용하여 동시에 여러 요청이 들어와도 하나만 처리되게 했습니다.


즉, 요청이 중복되거나 재처리되더라도 상태는 항상 동일하게 유지되도록 설계했습니다.


