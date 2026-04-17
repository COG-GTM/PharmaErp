/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pharma.erp.Validation;

import com.schema.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author tarang
 */
public class UserValidation implements Validation<User>{
    private static final Logger LOGGER = Logger.getLogger(UserValidation.class.getName());

    public enum TypeError{NULL_VALUE,ONLY_NUMBER,INVALID_CHARACTER,NO_ERROR,CONTAINS_SPACE, LENGTH_ERROR};
    private enum Present{PRESENT,NOT_PRESENT,DATA_ERROR};
    public enum PassWordTypeError{NULL_VALUE,INVALID,CONTAINS_SPACE,VALID,LENGTH_ERROR};
    private Data<User> _data_user;
    private Session _session;
    public UserValidation(Data<User> _data_user,Session _session) {
        this._data_user = _data_user;
        this._session = _session;
    }

    private void trimData(User _data){
        _data.setUsername(_data.getUsername().trim());
        _data.setPassword(_data.getPassword().trim());
    }

    public TypeError usernameValidation(User _data){
        trimData(_data);

        String _username = _data.getUsername();
        if(_username==null){
            return TypeError.NULL_VALUE;
        }
        if(_username.contains(" "))
            return TypeError.CONTAINS_SPACE;
        if(_username.matches("(\\d)+"))
            return TypeError.ONLY_NUMBER;
        if(!_username.matches("(\\w|\\d)+"))
            return TypeError.INVALID_CHARACTER;
        if(!_username.matches("(\\w|\\d){8,12}"))
            return TypeError.LENGTH_ERROR;
        else
            return TypeError.NO_ERROR;
    }
    public PassWordTypeError passwordValidation(User _data){
        trimData(_data);

        String _password = _data.getPassword();
        if(_password==null){
            return PassWordTypeError.NULL_VALUE;
        }
        LOGGER.fine("Password validation in progress");
        if(_password.contains(" ")){
            return PassWordTypeError.CONTAINS_SPACE;
        }
        if(!_password.matches(("(.{8,})"))){
            return PassWordTypeError.LENGTH_ERROR;
        }else{
            return PassWordTypeError.VALID;
        }
        
        
    }


    //Checks if username and password are according to given criteria

    public boolean isValid(User _data) {
        //Trim data before use
        trimData(_data);

        TypeError _user = usernameValidation(_data);
        PassWordTypeError _password = passwordValidation(_data);
        if(_user==TypeError.NO_ERROR&&_password==PassWordTypeError.VALID)
            return true;
        else
            return false;
   }

    @Override
    public boolean isInsertable(User _data) {

        return isValid(_data)&&!isPresent(_data);


    }
    @Override
    public boolean isRemovable(User _data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    //Matches the complete user object with the given parameter
    @Override
    public boolean isPresent(User _data) {
        _data.setUsername(_data.getUsername().trim());
        String _username = _data.getUsername();
        Query<User> q = _session.createQuery("FROM User U WHERE U.username = :username", User.class);
        q.setParameter("username", _username);
        List<User> result = q.getResultList();

        LOGGER.info("User lookup attempted for username: " + _username);

        if(result.size() > 0){
            return User.areEqual(_data, result.get(0));
        }else{
            return false;
        }
    }


    
    
}
