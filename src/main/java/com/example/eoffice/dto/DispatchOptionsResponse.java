package com.example.eoffice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DispatchOptionsResponse {
    private List<UserOption> officeChiefs;
    private List<UserOption> directors;
    private List<UserOption> leaders;
    private List<UserOption> allUsers;
    private List<DepartmentOption> departments;
    private List<TaskGroupOption> taskGroups;

    @Getter
    @AllArgsConstructor
    public static class UserOption {
        private Long id;
        private String fullName;
        private String email;
        private String departmentName;
        private String rolesLabel;
    }

    @Getter
    @AllArgsConstructor
    public static class DepartmentOption {
        private Long id;
        private String name;
        private Long headId;
        private String headName;
        private String headEmail;
        private Long deputyId;
        private String deputyName;
        private String deputyEmail;
        private List<UserOption> deputies;
        private List<UserOption> clerks;
    }

    @Getter
    @AllArgsConstructor
    public static class TaskGroupOption {
        private Long id;
        private String name;
        private int memberCount;
        private List<UserOption> members;
    }
}
