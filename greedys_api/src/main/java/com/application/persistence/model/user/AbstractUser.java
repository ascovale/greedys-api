package com.application.persistence.model.user;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractUser extends BaseUser{
    protected String name;
    protected String surname;
    protected String nickName;
    protected String email;
    @Column(length = 60)
    protected String password;
    protected String phoneNumber;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    @Override
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNickName(){ return nickName;}
    public void setNickName(String nickName){this.nickName=nickName;}

    @Override
    public String getPassword() { return password; }
    @Override
    public int hashCode() { return Objects.hash(email);}
    public void setPassword(String password) {
        this.password = password;
    }

}
