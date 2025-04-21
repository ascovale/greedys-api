package com.application.spring.dataloader;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.SetupConfigDAO;
import com.application.persistence.model.systemconfig.SetupConfig;
import com.application.service.AllergyService;
import com.application.web.dto.post.NewAllergyDTO;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SetupConfigDAO setupConfigDAO;
    @Autowired
    private AdminSetup adminSetup;
    @Autowired
    private CustomerSetup customerSetup;
    @Autowired
    private RestaurantDataLoader restaurantDataLoader;
    @Autowired
    private AllergyService allergyService;

    static final Logger logger = LoggerFactory.getLogger(SetupDataLoader.class);

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (!setupConfig.isAlreadySetup()) {
            logger.info(">>> --- Setup started --- <<<");
            setupConfig.setId(1L);
            setupConfig.setAlreadySetup(true);
            setupConfigDAO.save(setupConfig);
            restaurantDataLoader.createDefaultServiceTypes();
            adminSetup.setupAdminRolesAndPrivileges();
            customerSetup.setupCustomerRolesAndPrivileges();
            restaurantDataLoader.createRestaurantPrivilegesAndRoles();
            logger.info(">>> --- Setup finished --- <<<");
        }
        if (!setupConfig.isDataUploaded()) {
            logger.info(">>> --- Creating Test data --- <<<");
            setupConfig.setAlreadySetup(true);
            setupConfig.setDataUploaded(true);
            adminSetup.createSomeAdmin();
            customerSetup.createSomeCustomer();
            createAllergies();
            restaurantDataLoader.createRestaurantCategories();
            restaurantDataLoader.createRestaurantLaSoffittaRenovatio();
            restaurantDataLoader.assignCategoriesToLaSoffittaRenovatio();
            restaurantDataLoader.createAdditionalRestaurants();
            logger.info("    >>>  ---   Test data Created   ---  <<< ");
        }
    }

    @Transactional
    private void createAllergies() {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (setupConfig.isDataUploaded()) {
            logger.info(">>> --- Allergies already created, skipping --- <<<");
            return;
        }

        logger.info(">>> --- Creating Allergies --- <<<");
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
        logger.info(">>> --- Allergies Created --- <<<");
    }

}

