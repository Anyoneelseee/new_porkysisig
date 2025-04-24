package com.example.sisig;

public class MenuItem {
    private final String name;
    private final String serving;
    private final String description;
    private final int price;
    private final int imageResId; // Add the image resource ID field

    // Constructor
    public MenuItem(String name, String serving, String description, int price, int imageResId) {
        this.name = name;
        this.serving = serving;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId; // Initialize the image resource ID

    }

    // Getters
    public String getName() {
        return name;
    }

    public String getServing() {
        return serving;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public int getImageResId() {
        return imageResId; // Getter for the image resource ID
    }
}


