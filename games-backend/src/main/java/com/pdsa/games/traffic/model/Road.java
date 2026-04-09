package com.pdsa.games.traffic.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Roads")
public class Road {

    @EmbeddedId
    private RoadId id;

    @Column(name = "Capacity", nullable = false)
    private Integer capacity;

    public Road() {}

    public Road(RoadId id, Integer capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public RoadId getId() {
        return id;
    }

    public void setId(RoadId id) {
        this.id = id;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
