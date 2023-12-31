package com.ssafy.backend.global.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssafy.backend.domain.couple.Couple;
import com.ssafy.backend.domain.couple.repository.CoupleRepository;
import com.ssafy.backend.domain.notification.NotificationType;
import com.ssafy.backend.domain.notification.ReadStatus;
import com.ssafy.backend.domain.notification.dto.NotificationRegistDto;
import com.ssafy.backend.domain.notification.service.NotificationService;
import com.ssafy.backend.domain.schedule.Schedule;
import com.ssafy.backend.domain.schedule.repository.ScheduleRepository;
import com.ssafy.backend.domain.tipBox.TipCode;
import com.ssafy.backend.domain.tipBox.repository.TipCodeRepository;
import com.ssafy.backend.domain.user.User;
import com.ssafy.backend.domain.redis.fcm.FcmToken;
import com.ssafy.backend.domain.redis.fcm.FcmTokenRepository;
import com.ssafy.backend.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class NotificationScheduler {

    @Value("${project.properties.firebase-create-scoped}")
    String fireBaseCreateScoped;

    @Value("${project.properties.firebase-topic}")
    String topic;

    private FirebaseMessaging firebaseMessaging;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private FcmTokenRepository fcmTokenRepository;
    @Autowired
    private TipCodeRepository tipCodeRepository;
    @Autowired
    private CoupleRepository coupleRepository;
    @Autowired
    private UserRepository userRepository;

    private static List<Long> sqlTimeList = new ArrayList<>();
    private static List<Long> redTimeList = new ArrayList<>();


    @PostConstruct
    public void firebaseSetting() throws IOException {
        //내 firebase 콘솔에서 가져온 비공개 키 파일을 통해 백엔드에서 파이어베이스에 접속함
        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new ClassPathResource("firebase/blooming-18b74-firebase-adminsdk-i9dwm-372e530990.json").getInputStream())
                .createScoped((Arrays.asList(fireBaseCreateScoped)));
        FirebaseOptions secondaryAppConfig = FirebaseOptions.builder()
                .setCredentials(googleCredentials)
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(secondaryAppConfig);
        this.firebaseMessaging = FirebaseMessaging.getInstance(app);
    }

    //시간에 맞게 푸시 알림을 스케줄링하는 코드
//    @Scheduled(cron = "* * 1 * * ?")
    @Scheduled(cron = "0 0 9 * * ?")
//    @Scheduled(fixedDelay = 600000)
    public void pushMorningDietAlarm() {

        //여기서 일정 DB를 읽고 일정이 한 달, 삼 주, 일주일, 하루 전, 당일이면 알림을 보냄.
        //나중에 지난 일정은 삭제? 해도 될듯
        //일정 repository에서 day를 매개변수로 넘겨주면서, 30일, 21일, 7일, 1일, 0일 을 인자로 해서 date 비교해서 해당되는거 가져옴. 알림 보내고 테이블에 추가.
        // 일정 알림
        for (int day : new int[]{0, 1, 7, 30}) {
            System.out.println(day + "일 후 알림");

            //day일 후 스케줄을 일단 읽어옴 -> 푸시 알림 보내기 + 알림 로그 테이블에 저장
            List<Schedule> schedules = scheduleRepository.findAllByScheduleDate(LocalDate.now().plusDays(day));
            for (Schedule schedule : schedules) {
                System.out.println(schedule);

                //커플 아이디에 해당하는 유저 두 명을 찾는다. 남자는 신랑, 여자는 신부로 매핑한다.
                User groom = null;
                User bride = null;
                List<User> users = schedule.getCouple().getUsers();
                for (User user : users) {
                    if (user.getGender().equals("MALE")) {
                        groom = user;
                    } else if (user.getGender().equals("FEMALE")) {
                        bride = user;
                    }
                }

                //스케쥴 타입에 따라 다르게 알림 내용 처리
                String title = schedule.getScheduleDate() + " " + schedule.getTitle(); //알림 제목은 일단 같게
                String contentGroom = "";
                String contentBride = "";

                //null 참조 방지를 위해 닉네임 미리 받기
                String groomNickname = (groom != null) ? groom.getNickname() : "예비신랑";
                String brideNickname = (bride != null) ? bride.getNickname() : "예비신부";

                //오늘은 오늘이라고 하기
                String dayName = "오늘 ";
                if (day == 1) {
                    dayName = "내일 ";
                } else if (day != 0) {
                    dayName = day + "일 후 ";
                }
                if (schedule.getScheduledBy() == null) continue; //예외처리
                switch (schedule.getScheduledBy()) {
                    case COMMON:
                        //두 명에게 같은 알림 전송
                        contentGroom = dayName + "두 분의 " + schedule.getContent() + "이 있어요.";
                        contentBride = dayName + "두 분의 " + schedule.getContent() + "이 있어요.";
                        break;
                    case MALE:
                        //신랑 일정.
                        contentGroom = dayName + "개인 일정이 있어요.";
                        contentBride = dayName + groomNickname + "님의 개인 일정이 있어요.";
                        break;
                    case FEMALE:
                        //신부 일정.
                        contentGroom = dayName + brideNickname + "님의 개인 일정이 있어요.";
                        contentBride = dayName + "개인 일정이 있어요.";
                        break;
                }

                //처리한 내용을 알림 전송(신랑, 신부)
                log.info(sendNotificationByToken(new FCMNotificationRequestDto(groom, title, contentGroom)));
                log.info(sendNotificationByToken(new FCMNotificationRequestDto(bride, title, contentBride)));
            }
        }


        // 디데이 체크리스트 알림
        List<TipCode> tipCodeList = tipCodeRepository.findAll();
        for (TipCode tipCode : tipCodeList) {
            int day = tipCode.getLeftDay();
            System.out.println("D-Day" + day + "일 후 알림");

            //day일 후 체크리스트 일단 읽어옴 -> 푸시 알림 보내기 + 알림 로그 테이블에 저장
            List<Couple> couples = coupleRepository.findAllByWeddingDate(LocalDate.now().plusDays(day));
            for (Couple couple : couples) {
                System.out.println(couple);

                //커플 아이디에 해당하는 유저 두 명을 찾는다. 남자는 신랑, 여자는 신부로 매핑한다.
                User groom = null;
                User bride = null;
                List<User> users = couple.getUsers();
                for (User user : users) {
                    if (user.getGender().equals("MALE")) {
                        groom = user;
                    } else if (user.getGender().equals("FEMALE")) {
                        bride = user;
                    }
                }

                //스케쥴 타입에 따라 다르게 알림 내용 처리
                String contentGroom = "";
                String contentBride = "";

                String titleGroom = "";
                String titleBride = "";
                //null 참조 방지를 위해 닉네임 미리 받기
                String groomNickname = (groom != null) ? groom.getNickname() : "예비신랑";
                String brideNickname = (bride != null) ? bride.getNickname() : "예비신부";

                // 체크리스트 타입에 따라 다르게 알림 내용 처리
                if (day == 0) {
                    titleGroom = groomNickname + "님의 결혼을 축하해요";
                    titleBride = brideNickname + "님의 결혼을 축하해요";

                } else {
                    titleGroom = "결혼식까지" + day + "일 남았어요";
                    titleBride = "결혼식까지" + day + "일 남았어요";
                }


                if (day == 0) {
                    contentGroom = "두 분의 앞날이 더욱 행복하길 바래요❤";
                    contentBride = "두 분의 앞날이 더욱 행복하길 바래요❤";
                } else {
                    contentGroom = groomNickname + "님 " + tipCode.getTitle() + " 하셔야 해요. 지금 준비하러 가볼까요?";
                    contentBride = brideNickname + "님 " + tipCode.getTitle() + " 하셔야 해요. 지금 준비하러 가볼까요?";
                }

                //처리한 내용을 알림 전송(신랑, 신부)
                log.info(sendNotificationByToken(new FCMNotificationRequestDto(groom, titleGroom, contentGroom)));
                log.info(sendNotificationByToken(new FCMNotificationRequestDto(bride, titleBride, contentBride)));
            }
        }
    }


    public String sendNotificationByToken(FCMNotificationRequestDto fcmDto) {
        User user = fcmDto.getUser();

        if (user != null) {
            //0. 일림 로그 테이블에 저장 : 사용자마다, 알림 테이블에 저장 : 유저가 있으면 보내기
            notificationService.registNotification(new NotificationRegistDto(
                    ReadStatus.UNREAD,
                    NotificationType.SCHEDULE,
                    fcmDto.getTitle(),
                    fcmDto.getBody(),
                    fcmDto.getUser().getId()
            ));

            // 시간 측정
            long startTime = 0L;
            long endTime = 0L;
            Long fcmUserId = user.getId();

            // FCM 시간 측정
            startTime = System.currentTimeMillis();
            User fcmUser = userRepository.findById(fcmUserId)
                    .orElse(null);
            String sqlFcmToken = fcmUser.getFcmToken();
            endTime = System.currentTimeMillis();
            System.out.println("MySQL 시작 : "+startTime);
            System.out.println("MySQL 종료 : "+endTime);
            System.out.println("MySQL FCM Token 응답시간 : "+(endTime-startTime));
            sqlTimeList.add(endTime-startTime);
            System.out.println("SIZE = "+sqlTimeList.size());
            //user id를 통해 redis에서 받아오자 : 일단 테스트는 보류
            startTime = System.currentTimeMillis();
            FcmToken fcmToken = fcmTokenRepository.findById(String.valueOf(fcmUserId))
                    .orElse(null);
            endTime = System.currentTimeMillis();
            System.out.println("REDIS 시작 : "+startTime);
            System.out.println("REDIS 종료 : "+endTime);
            System.out.println("REDIS FCM Token 응답시간 : "+(endTime-startTime));
            redTimeList.add(endTime-startTime);

            if(sqlTimeList.size()%100 == 0) {
                System.out.println("********************************************************");
                System.out.println("SQL FCM SIZE : "+sqlTimeList.size());
                System.out.println("********************************************************");
            }

            // 타임 리스트 사이즈 1000, 5000, 10000 평균 내기
            if(sqlTimeList.size()==1000 || sqlTimeList.size()==5000 || sqlTimeList.size()==10000){
                Long sqlSum = 0L;
                Long redSum = 0L;
                for(Long l : sqlTimeList)
                    sqlSum+=l;
                for(Long l : redTimeList)
                    redSum+=l;

                System.out.println("************************************************************");
                System.out.println("알림 "+sqlTimeList.size()+"번 전송시 MySQL, Redis FCM TOKEN 조회 평균 시간");
                System.out.println("MySQL FCM Token 응답시간 : "+(sqlSum/sqlTimeList.size()));
                System.out.println("REDIS FCM Token 응답시간 : "+(redSum/redTimeList.size()));
                System.out.println("************************************************************");

            }

            if (fcmToken != null && user.getNotificationSetting().equals("agree")) {
                String token = fcmToken.getValue(); //redis에서 토큰 읽어온거

                Notification notification = Notification.builder()
                        .setTitle(fcmDto.getTitle())
                        .setBody(fcmDto.getBody())
                        .build();

                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(notification)
                        .build();

                try {
                    firebaseMessaging.send(message);
                    return "알림 전송 성공 " + fcmDto.getUser();
                } catch (FirebaseMessagingException e) {
                    e.printStackTrace();
                    return "알림 전송 실패 " + fcmDto.getUser();
                }
            } else {
                return "Redis에 유저 FCM token 없음 " + fcmDto.getUser();
            }
        } else {
            return "해당 유저 없음 " + fcmDto.getUser();
        }
    }
}
