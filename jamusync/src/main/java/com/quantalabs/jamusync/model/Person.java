package com.quantalabs.jamusync.model;

/**
 * Person is a classic textbook inheritance example: "a Person IS-A BaseModel".
 *
 * A Person is any human in our system (for now that means a User). Every
 * person has a full name, so we put "fullName" here. Because Person extends
 * BaseModel, a Person also automatically has an "id" and a "createdAt"
 * inherited from BaseModel - we did not have to write them again.
 *
 * Person is still "abstract" because "a person" is too general to create on
 * its own; we always create a specific kind of person, such as a User.
 * Notice Person does NOT implement getSummary() - because it is abstract, it
 * can pass that responsibility down to its own children (User).
 */
public abstract class Person extends BaseModel {

    protected String fullName;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
