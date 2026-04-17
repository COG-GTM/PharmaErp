package com.pharma.erp.Validation;

import com.schema.User;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Data generic wrapper class.
 */
public class DataTest {

    @Test
    public void testConstructorAndGetters() {
        Data<User> data = new Data<User>(null, User.class);
        assertNull(data.getFactory());
        assertEquals(User.class, data.getData_class());
    }

    @Test
    public void testSetters() {
        Data<User> data = new Data<User>(null, User.class);
        data.setData_class(User.class);
        assertEquals(User.class, data.getData_class());
    }

    @Test
    public void testToString() {
        Data<User> data = new Data<User>(null, User.class);
        String result = data.toString();
        assertNotNull(result);
        assertTrue(result.contains("Data{"));
        assertTrue(result.contains("User"));
    }
}
