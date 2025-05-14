import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
public class InitialDataLoader {

    private final RoleRepository roleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private static final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);

    @Bean
    public CommandLineRunner loadInitialData() {
        return args -> {
            logger.info("Başlangıç verileri yükleniyor...");
            initializeRoles();
            initializeSubscriptionPlans();
        };
    }

    @Transactional
    protected void initializeRoles() {
        createRoleIfNotExists("ROLE_ADMIN", "Sistem Yöneticisi");
        createRoleIfNotExists("ROLE_USER", "Standart Kullanıcı");
        createRoleIfNotExists("ROLE_COMPANY_ADMIN", "Şirket Yöneticisi");
    }

    @Transactional
    protected void initializeSubscriptionPlans() {
        // Ücretsiz Deneme Planı
        createSubscriptionPlanIfNotExists(
            SubscriptionPlan.builder()
                .name("FREE_TRIAL")
                .displayName("Ücretsiz Deneme")
                .description("30 günlük ücretsiz deneme sürümü")
                .price(BigDecimal.ZERO)
                .billingInterval(BillingInterval.MONTHLY)
                .trialDays(30)
                .features(Set.of("BASIC_FEATURES", "LIMITED_USERS_5"))
                .isPublic(true)
                .status(PlanStatus.ACTIVE)
                .build()
        );

        // Temel Plan
        createSubscriptionPlanIfNotExists(
            SubscriptionPlan.builder()
                .name("BASIC_MONTHLY")
                .displayName("Temel Paket")
                .description("Küçük işletmeler için uygun paket")
                .price(new BigDecimal("199.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .features(Set.of("BASIC_FEATURES", "STANDARD_SUPPORT", "MAX_USERS_10"))
                .isPublic(true)
                .status(PlanStatus.ACTIVE)
                .build()
        );

        // Premium Plan
        createSubscriptionPlanIfNotExists(
            SubscriptionPlan.builder()
                .name("PREMIUM_MONTHLY")
                .displayName("Premium Paket")
                .description("Orta ve büyük ölçekli işletmeler için gelişmiş özellikler")
                .price(new BigDecimal("499.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .features(Set.of("PREMIUM_FEATURES", "PRIORITY_SUPPORT", "UNLIMITED_USERS"))
                .isPublic(true)
                .status(PlanStatus.ACTIVE)
                .build()
        );
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (!roleRepository.findByName(roleName).isPresent()) {
            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .build();
            roleRepository.save(role);
            logger.info("'{}' rolü oluşturuldu", roleName);
        }
    }

    private void createSubscriptionPlanIfNotExists(SubscriptionPlan plan) {
        if (!subscriptionPlanRepository.findByName(plan.getName()).isPresent()) {
            subscriptionPlanRepository.save(plan);
            logger.info("'{}' abonelik planı oluşturuldu", plan.getName());
        }
    }
}