package com.application.common.spring.dataloader;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.admin.AdminSetup;
import com.application.common.persistence.dao.SetupConfigDAO;
import com.application.common.persistence.model.systemconfig.SetupConfig;
import com.application.common.service.AllergyService;
import com.application.common.web.dto.customer.NewAllergyDTO;
import com.application.customer.CustomerSetup;
import com.application.restaurant.RestaurantDataLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final SetupConfigDAO setupConfigDAO;
    private final AdminSetup adminSetup;
    private final CustomerSetup customerSetup;
    private final RestaurantDataLoader restaurantDataLoader;
    private final AllergyService allergyService;
    private final Environment env;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        // Attende che il database sia completamente pronto (come in Docker)
        waitForDatabaseReady();
        
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (!setupConfig.isAlreadySetup()) {
            log.info("🚀 --- Setup started --- ");
            setupConfig.setId(1L);
            setupConfig.setAlreadySetup(true);
            setupConfigDAO.save(setupConfig);
            restaurantDataLoader.createDefaultServiceTypes();
            adminSetup.setupAdminRolesAndPrivileges();
            customerSetup.setupCustomerRolesAndPrivileges();
            restaurantDataLoader.createRestaurantPrivilegesAndRoles();
            log.info("✅ --- Setup finished --- ");
        }
        if (!setupConfig.isDataUploaded()) {
            log.info("📊 --- Creating Test data --- ");
            setupConfig.setAlreadySetup(true);
            setupConfig.setDataUploaded(true);
            adminSetup.createSomeAdmin();
            customerSetup.createSomeCustomer();
            createAllergies();
            restaurantDataLoader.createRestaurantCategories();
            restaurantDataLoader.createRestaurantLaSoffittaRenovatio();
            restaurantDataLoader.assignCategoriesToLaSoffittaRenovatio();
            log.info("✅ --- Test data Created --- ");
        }
    }

    @Transactional
    private void createAllergies() {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (setupConfig.isDataUploaded()) {
            log.info("ℹ️ --- Allergies already created, skipping --- ");
            return;
        }

        log.info("🦠 --- Creating Allergies --- ");
        List<NewAllergyDTO> allergies = Arrays.asList(
            new NewAllergyDTO("Cereals", "Includes wheat, rye, barley, oats, and foods like bread, pasta, and cereals."),
            new NewAllergyDTO("Shellfish", "Includes shrimp, crab, lobster, and other crustaceans."),
            new NewAllergyDTO("Eggs", "Includes eggs and foods containing eggs such as mayonnaise and baked goods."),
            new NewAllergyDTO("Fish", "Includes fish like salmon, tuna, and cod."),
            new NewAllergyDTO("Peanuts", "Includes peanuts and foods containing peanuts such as peanut butter."),
            new NewAllergyDTO("Soy", "Includes soybeans and soy-based products like tofu and soy milk."),
            new NewAllergyDTO("Milk", "Includes milk and dairy products like cheese, butter, and yogurt."),
            new NewAllergyDTO("Nuts", "Includes tree nuts like almonds, walnuts, and hazelnuts."),
            new NewAllergyDTO("Celery", "Includes celery and celery-based products like celery salt."),
            new NewAllergyDTO("Mustard", "Includes mustard seeds and mustard-based products."),
            new NewAllergyDTO("Sesame", "Includes sesame seeds and sesame oil."),
            new NewAllergyDTO("Sulfites", "Includes sulfites found in dried fruits, wine, and some processed foods."),
            new NewAllergyDTO("Lupin", "Includes lupin beans and lupin-based flour."),
            new NewAllergyDTO("Mollusks", "Includes clams, mussels, oysters, and squid."),
            new NewAllergyDTO("Gluten", "Includes foods containing gluten such as bread, pasta, and pastries."),
            new NewAllergyDTO("Corn", "Includes corn and corn-based products."),
            new NewAllergyDTO("Garlic", "Includes garlic and foods containing garlic."),
            new NewAllergyDTO("Onion", "Includes onion and foods containing onion."),
            new NewAllergyDTO("Pork", "Includes pork and pork-based products."),
            new NewAllergyDTO("Beef", "Includes beef and beef-based products."),
            new NewAllergyDTO("Chicken", "Includes chicken and chicken-based products."),
            new NewAllergyDTO("Alcohol", "Includes alcoholic beverages and foods containing alcohol."));

        for (NewAllergyDTO allergy : allergies) {
            if (allergyService.findByName(allergy.getName()) == null) {
                allergyService.createAllergy(allergy);
            }
        }

        setupConfig.setDataUploaded(true);
        setupConfigDAO.save(setupConfig);
        log.info("✅ --- Allergies Created --- ");
    }

    /**
     * Attende che il database sia completamente pronto, simile alla logica Docker
     */
    private void waitForDatabaseReady() {
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        
        // Gestione diversa per ogni profilo
        if (activeProfiles.contains("dev-minimal")) {
            waitForH2Database();
        } else if (activeProfiles.contains("dev")) {
            waitForMySQLDevDatabase();
        }
        // Per docker/prod non serve attesa, il container assicura che MySQL sia pronto
    }
    
    /**
     * Attesa specifica per H2 con profilo dev-minimal
     */
    private void waitForH2Database() {
        log.info("🔄 Attendo che H2 e Hibernate siano completamente pronti...");
        int attempts = 15; // Ridotto per H2 che è più veloce
        while (attempts > 0) {
            try {
                // Prima verifica la connessione al database
                var connection = dataSource.getConnection();
                
                // Verifica semplificata per H2: solo tabelle principali
                var stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('SETUP_CONFIG', 'ADMIN', 'CUSTOMER')"
                );
                var rs = stmt.executeQuery();
                rs.next();
                int tableCount = rs.getInt(1);
                
                rs.close();
                stmt.close();
                connection.close();
                
                if (tableCount >= 3) { // Verifica solo le tabelle principali per H2
                    log.info("✅ Database H2 e schema Hibernate pronti (tabelle: {}).", tableCount);
                    Thread.sleep(200); // Piccola attesa finale
                    break;
                } else {
                    throw new RuntimeException("Tabelle principali non ancora create. Trovate: " + tableCount + "/3");
                }
                
            } catch (Exception ex) {
                attempts--;
                log.info("⏳ Database H2 non ancora pronto: {}. Riprovo tra 1 secondo... Tentativi rimasti: {}", 
                        ex.getMessage(), attempts);
                try {
                    Thread.sleep(1000); // Attesa ridotta per H2
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrotto mentre si attende il DB.", ie);
                }
            }
        }
        if (attempts == 0) {
            throw new IllegalStateException("Database H2 e schema Hibernate non completamente disponibili dopo molteplici tentativi.");
        }
    }
    
    /**
     * Attesa specifica per MySQL locale con profilo dev
     */
    private void waitForMySQLDevDatabase() {
        log.info("🔄 Attendo che MySQL locale per sviluppo sia completamente pronto...");
        int attempts = 10;
        while (attempts > 0) {
            try {
                // Verifica la connessione al database MySQL
                var connection = dataSource.getConnection();
                
                // Verifica che le tabelle critiche esistano per MySQL
                var stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN ('setup_config', 'admin', 'customer', 'slot')"
                );
                stmt.setString(1, connection.getCatalog()); // Nome del database corrente
                var rs = stmt.executeQuery();
                rs.next();
                int tableCount = rs.getInt(1);
                
                rs.close();
                stmt.close();
                connection.close();
                
                if (tableCount >= 3) {
                    log.info("✅ Database MySQL locale per sviluppo completamente pronto (trovate {} tabelle critiche).", tableCount);
                    Thread.sleep(500);
                    break;
                } else {
                    throw new RuntimeException("Tabelle MySQL non ancora create. Trovate: " + tableCount + "/3");
                }
                
            } catch (Exception ex) {
                attempts--;
                log.info("⏳ MySQL locale non ancora pronto: {}. Riprovo tra 1 secondo... Tentativi rimasti: {}", 
                        ex.getMessage(), attempts);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrotto mentre si attende MySQL.", ie);
                }
            }
        }
        if (attempts == 0) {
            throw new IllegalStateException("Database MySQL locale non completamente disponibile dopo molteplici tentativi.");
        }
    }

}

