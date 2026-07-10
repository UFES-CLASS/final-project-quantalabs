package com.quantalabs.jamusync.model;

/**
 * BaseModel is the PARENT class for every data model in the app.
 *
 * Almost every model we store in the database shares two things:
 *   - an "id" (its unique number in the database)
 *   - a "createdAt" (the date/time it was created)
 *
 * Instead of copy-pasting these two fields and their getters/setters into
 * Product, Voucher, Transaction, User, etc., we write them ONCE here and let
 * the other models INHERIT them by using the "extends" keyword. This is the
 * core idea of inheritance in Object-Oriented Programming (OOP): write shared
 * code in a parent class so the children don't have to repeat it.
 *
 * The class is "abstract" because a plain BaseModel on its own has no real
 * meaning - you never create just a "BaseModel", you always create a specific
 * thing like a Product or a User.
 */
public abstract class BaseModel {

    // "protected" means these fields are visible to this class AND to any child
    // class that extends BaseModel (like Product or User).
    protected int id;
    protected String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * An abstract method has NO body here - just a signature ending in ";".
     * By declaring it abstract we force every child class to write its own
     * version of getSummary(). This lets us ask any model "describe yourself"
     * and each one answers in its own way (this is called polymorphism).
     */
    public abstract String getSummary();
}
