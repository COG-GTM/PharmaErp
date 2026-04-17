package com.schema;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for User entity.
 */
public class UserTest {

    @Test
    public void testConstructorAndGetters() {
        User user = new User("testuser", "testpass");
        assertEquals("testuser", user.getUsername());
        assertEquals("testpass", user.getPassword());
    }

    @Test
    public void testDefaultConstructor() {
        User user = new User();
        assertNull(user.getUsername());
        assertNull(user.getPassword());
    }

    @Test
    public void testSetters() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("newpass");
        assertEquals("newuser", user.getUsername());
        assertEquals("newpass", user.getPassword());
    }

    @Test
    public void testAreEqual_bothMatch() {
        User a = new User("user1", "pass1");
        User b = new User("user1", "pass1");
        assertTrue(User.areEqual(a, b));
    }

    @Test
    public void testAreEqual_usernameMismatch() {
        User a = new User("user1", "pass1");
        User b = new User("user2", "pass1");
        assertFalse(User.areEqual(a, b));
    }

    @Test
    public void testAreEqual_passwordMismatch() {
        User a = new User("user1", "pass1");
        User b = new User("user1", "pass2");
        assertFalse(User.areEqual(a, b));
    }

    @Test
    public void testAreEqual_nullUsername() {
        User a = new User(null, "pass1");
        User b = new User("user1", "pass1");
        assertFalse(User.areEqual(a, b));
    }

    @Test
    public void testAreEqual_nullPassword() {
        User a = new User("user1", null);
        User b = new User("user1", "pass1");
        assertFalse(User.areEqual(a, b));
    }

    @Test
    public void testAreEqual_bothNull() {
        User a = new User(null, null);
        User b = new User(null, null);
        assertFalse(User.areEqual(a, b));
    }

    @Test
    public void testToString() {
        User user = new User("testuser", "testpass");
        String result = user.toString();
        assertTrue(result.contains("testuser"));
        assertFalse(result.contains("testpass"));
    }
}
