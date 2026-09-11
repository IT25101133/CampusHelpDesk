package com.sliit.helpdesk.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String department;

    @Min(1)
    private int slaHours = 48;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(int slaHours) {
        this.slaHours = slaHours;
    }
}
