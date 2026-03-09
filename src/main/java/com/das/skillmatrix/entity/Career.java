package com.das.skillmatrix.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "careers")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class Career extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long careerId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneralStatus status = GeneralStatus.ACTIVE;

    private LocalDateTime deletedAt;

    private LocalDateTime deActiveAt;

    @JsonIgnore
    @NotAudited
    @OneToMany(mappedBy = "career", fetch = FetchType.LAZY)
    private List<Department> departments = new ArrayList<>();

    @NotAudited
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "career_managers", joinColumns = @JoinColumn(name = "career_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> managers = new ArrayList<>();
}
