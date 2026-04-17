package com.schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="user")
public class User {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    public User(){
        username = null;
        password = null;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{username='" + username + "'}";
    }

    public static boolean areEqual(User _main, User _compare){
        if(_main.getUsername() == null || _compare.getUsername() == null
                || _main.getPassword() == null || _compare.getPassword() == null){
            return false;
        }else{
            return _main.getUsername().equals(_compare.getUsername())
                    && _main.getPassword().equals(_compare.getPassword());
        }
    }
}
