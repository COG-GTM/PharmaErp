package MainApp;

import com.schema.User;
import org.hibernate.SessionFactory;
import com.pharma.erp.Database.SessionFactory_Helper;
import com.pharma.erp.Validation.Data;
import com.pharma.erp.Validation.UserValidation;
import org.hibernate.Session;

import java.util.logging.Logger;

/**
 * Main application entry point for PharmaErp system.
 */
public class MainApp {
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    public static void main(String[] args){
        SessionFactory _user_factory = SessionFactory_Helper.getSessionFactory(User.class, null, SessionFactory_Helper.Type.MAIN);
        Session _session = _user_factory.getCurrentSession();
        User user = new User("tarang", "parisdkh");
        _session.beginTransaction();
        UserValidation userValidation = new UserValidation(new Data<User>(_user_factory, User.class), _session);
        LOGGER.info("User present: " + userValidation.isPresent(user));
    }
}
