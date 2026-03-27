package com.das.skillmatrix.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneralStatus status = GeneralStatus.ACTIVE;

    @OneToMany(mappedBy = "position", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, orphanRemoval = true)
    private List<PositionSkill> positionSkills;
}
