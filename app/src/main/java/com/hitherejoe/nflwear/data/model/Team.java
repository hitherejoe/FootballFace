package com.hitherejoe.nflwear.data.model;

public class Team {
    public String name;
    public int logoResource;
    public int logoResourceSmall;
    public int color;

    public Team(String name, int logoResource, int resourceSmall, int color) {
        this.name = name;
        this.logoResource = logoResource;
        this.logoResourceSmall = resourceSmall;
        this.color = color;
    }
}
