/*
 * 实验室预约回归测试；测试夹具与生产配置分离。
 * 覆盖场景：concurrentOverlappingCreatesPersistOnePendingAndOneConflict。
 * 主要协作组件：ReservationPersistenceMapper。
 */
package org.example.backend;

import org.apache.ibatis.session.SqlSessionFactory;
import org.example.backend.mapper.ReservationInsertRow;
import org.example.backend.mapper.ReservationOverlapCount;
import org.example.backend.mapper.ReservationPersistenceMapper;
import org.example.backend.service.*;
import org.example.backend.entity.ReservationStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(named = "reservation.mysql.it", matches = "true")
class MySqlReservationConcurrencyIntegrationTest {
    private static final String SCHEMA = "smart_lab_reservation_it_" + UUID.randomUUID().toString().replace("-", "");
    private static final String ROOT_URL = System.getProperty("reservation.mysql.url", "jdbc:mysql://localhost:3306/");
    private static final String USERNAME = System.getProperty("reservation.mysql.username", "root");
    private static final String PASSWORD = System.getProperty("reservation.mysql.password", "root");

    @BeforeAll
    static void createSchema() throws SQLException {
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("create database `" + SCHEMA + "` character set utf8mb4");
            statement.execute("""
                    create table `%s`.lab_reservation (
                      id bigint primary key auto_increment,
                      applicant_user_id bigint not null,
                      applicant_name varchar(100) not null,
                      department varchar(100) not null,
                      contact varchar(50) not null,
                      lab_id bigint not null,
                      reservation_date date not null,
                      time_range varchar(20) not null,
                      scene varchar(100) not null,
                      attendee_count int not null,
                      reason varchar(255) not null,
                      status varchar(30) not null,
                      conflict_flag tinyint(1) not null,
                      reviewer_name varchar(100),
                      review_remark varchar(255),
                      created_at datetime not null default current_timestamp,
                      updated_at datetime not null default current_timestamp on update current_timestamp,
                      index idx_reservation_overlap (lab_id, reservation_date, status)
                    ) engine=InnoDB
                    """.formatted(SCHEMA));
        }
    }

    @AfterAll
    static void dropSchema() throws SQLException {
        if (!SCHEMA.startsWith("smart_lab_reservation_it_")) throw new IllegalStateException("unsafe schema name");
        try (Connection connection = DriverManager.getConnection(ROOT_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("drop database `" + SCHEMA + "`");
        }
    }

    @Test
    void concurrentOverlappingCreatesPersistOnePendingAndOneConflict() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            ReservationCommandHandler handler = context.getBean(ReservationCommandHandler.class);
            CreateReservationCommand command = new CreateReservationCommand("王老师", "计算机学院", "13800000000", 7L,
                    LocalDate.of(2026, 10, 8), "09:00-11:00", "课程", 30, "并发测试");
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<ReservationView>> futures = List.of(
                        executor.submit(() -> { start.await(); return handler.create(command, ReservationActor.teacher(101L)); }),
                        executor.submit(() -> { start.await(); return handler.create(command, ReservationActor.teacher(102L)); })
                );
                start.countDown();
                List<ReservationStatus> statuses = new ArrayList<>();
                for (Future<ReservationView> future : futures) statuses.add(future.get(15, TimeUnit.SECONDS).status());
                statuses.sort(Comparator.comparing(Enum::name));
                assertEquals(List.of(ReservationStatus.CONFLICT, ReservationStatus.PENDING), statuses);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource(ROOT_URL + SCHEMA + "?useSSL=false&allowPublicKeyRetrieval=true", USERNAME, PASSWORD);
        }
        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setMapperLocations(new ClassPathResource("mapper/ReservationPersistenceMapper.xml"));
            return Objects.requireNonNull(factory.getObject());
        }
        @Bean ReservationPersistenceMapper reservationPersistenceMapper(SqlSessionFactory factory) throws Exception {
            MapperFactoryBean<ReservationPersistenceMapper> mapper = new MapperFactoryBean<>(ReservationPersistenceMapper.class);
            mapper.setSqlSessionFactory(factory);
            return Objects.requireNonNull(mapper.getObject());
        }
        @Bean ReservationPersistenceService reservationPersistenceService(ReservationPersistenceMapper mapper) {
            return new ReservationPersistenceService(new BarrierPersistenceMapper(mapper));
        }
        @Bean LabCatalogQuery labCatalogQuery() {
            LabView laboratory = new LabView(7L, "LAB-7", "并发实验室", null, null, null, null,
                    50, null, null, null, 0, null, null, null);
            return new LabCatalogQuery() {
                public List<LabView> list(LabAccessScope scope) { return List.of(laboratory); }
                public LabView findById(long id, LabAccessScope scope) { return id == 7L ? laboratory : null; }
            };
        }
        @Bean LabAvailabilityQuery labAvailabilityQuery() {
            return (labId, count, startsAt, endsAt) ->
                    CapabilityResult.available(new LabAvailabilityView(labId, 50, true));
        }
        @Bean ScheduleOccupancyQuery scheduleOccupancyQuery() {
            return request -> CapabilityResult.available(new ScheduleOccupancyView(false));
        }
        @Bean ReservationLabService reservationLabService(LabCatalogQuery catalog, LabAvailabilityQuery availability) {
            return new ReservationLabService(catalog, availability);
        }
        @Bean ReservationScheduleService reservationScheduleService(ScheduleOccupancyQuery schedules, LabCatalogQuery catalog) {
            return new ReservationScheduleService(schedules, catalog);
        }
        @Bean ReservationCatalogQuery reservationCatalogQuery(ReservationPersistenceService reservations,
                                                                ReservationLabService laboratories) {
            return new DefaultReservationCatalogQuery(reservations, laboratories);
        }
        @Bean ReservationAvailabilityEvaluator availability(ReservationPersistenceService reservations,
                                                              ReservationLabService laboratories,
                                                              ReservationScheduleService schedules) {
            return new DefaultReservationAvailabilityEvaluator(reservations, laboratories, schedules);
        }
        @Bean DefaultReservationCreationService creation(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                                          ReservationLabService laboratories,
                                                          ReservationAvailabilityEvaluator availability) {
            return new DefaultReservationCreationService(reservations, catalog, laboratories, availability);
        }
        @Bean DefaultReservationReviewService review(ReservationPersistenceService reservations, ReservationCatalogQuery catalog,
                                                      ReservationAvailabilityEvaluator availability) {
            return new DefaultReservationReviewService(reservations, catalog, availability);
        }
        @Bean ReservationCommandHandler handler(DefaultReservationCreationService creation, DefaultReservationReviewService review) {
            return new DefaultReservationCommandHandler(creation, review);
        }
    }

    private static final class BarrierPersistenceMapper implements ReservationPersistenceMapper {
        private final ReservationPersistenceMapper delegate;
        private final CountDownLatch firstChecks = new CountDownLatch(2);
        private final AtomicInteger checks = new AtomicInteger();
        private BarrierPersistenceMapper(ReservationPersistenceMapper delegate) { this.delegate = delegate; }
        public List<ReservationRecord> findAll(ReservationRecordScope scope) { return delegate.findAll(scope); }
        public ReservationRecord findOne(long id, ReservationRecordScope scope) { return delegate.findOne(id, scope); }
        public int insert(ReservationInsertRow row) { return delegate.insert(row); }
        public ReservationOverlapCount countActiveOverlaps(ReservationOverlapCheck check) {
            ReservationOverlapCount result = delegate.countActiveOverlaps(check);
            if (checks.incrementAndGet() <= 2) {
                firstChecks.countDown();
                try { if (!firstChecks.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrent checks did not meet"); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
            }
            return result;
        }
        public int updateReview(ReservationReviewUpdate update) { return delegate.updateReview(update); }
    }
}
