package com.das.skillmatrix.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @NotAudited
    private String passwordHash;

    private String fullName;

    private String userAvatar;
    private String assessmentHistory;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, orphanRemoval = true)
    private List<UserSkill> userSkills;

    private String role; // ADMIN, MANAGER_CAREER, MANAGER_DEPARTMENT, MANAGER_TEAM, EMPLOYEE

    @JsonIgnore
    @NotAudited
    @ManyToMany(mappedBy = "managers")
    private List<Career> managedCareers = new ArrayList<>();

    @JsonIgnore
    @NotAudited
    @ManyToMany(mappedBy = "managers")
    private List<Department> managedDepartments = new ArrayList<>();

    @JsonIgnore
    @NotAudited
    @ManyToMany(mappedBy = "managers")
    private List<Team> managedTeams = new ArrayList<>();
}
