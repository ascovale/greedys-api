package com.application.spring.dataloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.application.persistence.dao.SetupConfigDAO;
import com.application.persistence.model.systemconfig.SetupConfig;

@Component
public class SetupDataLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private SetupConfigDAO setupConfigDAO;
    @Autowired
    private AdminSetup adminSetup;
    @Autowired
    private CustomerSetup customerSetup;

    @Override
    @Transactional
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        SetupConfig setupConfig = setupConfigDAO.findById(1L).orElse(new SetupConfig());
        if (!setupConfig.isAlreadySetup()) {
            setupConfig.setId(1L);
            setupConfig.setAlreadySetup(true);
            setupConfigDAO.save(setupConfig);
            adminSetup.setupAdminRolesAndPrivileges();
            customerSetup.setupCustomerRolesAndPrivileges();
        }

        if (!setupConfig.isDataUploaded()) {
            setupConfig.setDataUploaded(true);
            adminSetup.createSomeAdmin();
            customerSetup.createSomeCustomer();
        }
    }
}

