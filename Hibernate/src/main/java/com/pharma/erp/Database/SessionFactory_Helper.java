package com.pharma.erp.Database;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.logging.Logger;

/**
 * Helper class for creating Hibernate SessionFactory instances.
 */
public class SessionFactory_Helper {
    private static final Logger LOGGER = Logger.getLogger(SessionFactory_Helper.class.getName());

    public enum Type{MAIN, WEB}

    public static SessionFactory getSessionFactory(Class _class, String _path_configuration, Type _type){
        SessionFactory _factory;
        Configuration config = new Configuration();

        if(_path_configuration != null){
            config.configure(_path_configuration);
        } else {
            config.configure();
        }

        _factory = config.addAnnotatedClass(_class).buildSessionFactory();
        LOGGER.info("SessionFactory created for class: " + _class.getSimpleName() + ", type: " + _type);
        return _factory;
    }
}
