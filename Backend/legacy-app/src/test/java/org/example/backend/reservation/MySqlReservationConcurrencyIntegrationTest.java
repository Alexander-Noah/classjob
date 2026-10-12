package org.example.backend.reservation;

import org.apache.ibatis.session.SqlSessionFactory;
import org.example.backend.kernel.capability.CapabilityResult;
import org.example.backend.reservation.infrastructure.MyBatisReservationPersistenceAdapter;
import org.example.backend.reservation.infrastructure.ReservationPersistenceMapper;
import org.example.backend.reservation.api.ReservationActor;
import org.example.backend.reservation.api.ReservationCatalogQuery;
import org.example.backend.reservation.api.ReservationView;
import org.example.backend.reservation.application.*;
import org.example.backend.reservation.application.*;
import org.example.backend.reservation.domain.ReservationStatus;
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
        @Bean ReservationPersistencePort reservationPersistencePort(ReservationPersistenceMapper mapper) {
            return new BarrierPersistencePort(new MyBatisReservationPersistenceAdapter(mapper));
        }
        @Bean ManagedLaboratoryIdsPort managedLaboratoryIdsPort() { return userId -> Set.of(7L); }
        @Bean LaboratorySummaryPort laboratorySummaryPort() {
            return ids -> ids.contains(7L) ? Map.of(7L, new LaboratorySummary("并发实验室", "LAB-7")) : Map.of();
        }
        @Bean ReservationCatalogQuery reservationCatalogQuery(ReservationPersistencePort port, ManagedLaboratoryIdsPort managed,
                                                                LaboratorySummaryPort summaries) {
            return new DefaultReservationCatalogQuery(port, managed, summaries);
        }
        @Bean ReservationAvailabilityEvaluator availability(ReservationPersistencePort port) {
            return new DefaultReservationAvailabilityEvaluator(port,
                    (labId, count, range) -> CapabilityResult.available(new LabAvailabilitySnapshot(labId, 50, true)),
                    (labId, date, range) -> CapabilityResult.available(new ScheduleConflictSnapshot(labId, date, range, false)));
        }
        @Bean DefaultReservationCreationService creation(ReservationPersistencePort port, ReservationCatalogQuery catalog,
                                                          ManagedLaboratoryIdsPort managed, ReservationAvailabilityEvaluator availability) {
            return new DefaultReservationCreationService(port, catalog, managed, availability);
        }
        @Bean DefaultReservationReviewService review(ReservationPersistencePort port, ReservationCatalogQuery catalog,
                                                      ReservationAvailabilityEvaluator availability) {
            return new DefaultReservationReviewService(port, catalog, availability);
        }
        @Bean ReservationCommandHandler handler(DefaultReservationCreationService creation, DefaultReservationReviewService review) {
            return new DefaultReservationCommandHandler(creation, review);
        }
    }

    private static final class BarrierPersistencePort implements ReservationPersistencePort {
        private final ReservationPersistencePort delegate;
        private final CountDownLatch firstChecks = new CountDownLatch(2);
        private final AtomicInteger checks = new AtomicInteger();
        private BarrierPersistencePort(ReservationPersistencePort delegate) { this.delegate = delegate; }
        public List<ReservationRecord> findAll(ReservationRecordScope scope) { return delegate.findAll(scope); }
        public Optional<ReservationRecord> findOne(long id, ReservationRecordScope scope) { return delegate.findOne(id, scope); }
        public long insert(NewReservationRecord record) { return delegate.insert(record); }
        public long countActiveOverlaps(ReservationOverlapCheck check) {
            long result = delegate.countActiveOverlaps(check);
            if (checks.incrementAndGet() <= 2) {
                firstChecks.countDown();
                try { if (!firstChecks.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("concurrent checks did not meet"); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
            }
            return result;
        }
        public boolean updateReview(ReservationReviewUpdate update) { return delegate.updateReview(update); }
    }
}
