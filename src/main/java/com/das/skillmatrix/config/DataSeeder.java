package com.das.skillmatrix.config;

import com.das.skillmatrix.entity.*;
import com.das.skillmatrix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final UserRepository userRepository;
    private final CareerRepository careerRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final PositionRepository positionRepository;
    private final SkillRepository skillRepository;
    private final PositionSkillRepository positionSkillRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            // ========== 1. Seed Hierarchy: Career → Department → Team → Position → Skill ==========
            Career itCareer = seedCareer("IT Career", "Information Technology Career", "Technical");
            Career hrCareer = seedCareer("HR Career", "Human Resources Career", "Business");

            Department devDept = seedDepartment("Software Development", "Development Department", itCareer);
            Department qaDept = seedDepartment("Quality Assurance", "QA Department", itCareer);
            Department recruitDept = seedDepartment("Recruitment", "Talent Acquisition", hrCareer);
            Department erDept = seedDepartment("Employee Relations", "Employee Relations", hrCareer);

            seedTeam("Backend Team", "Java, Go, Python", devDept);
            seedTeam("Frontend Team", "React, Vue", devDept);
            seedTeam("Mobile Team", "iOS, Android", devDept);
            seedTeam("DevOps Team", "AWS, CI/CD", devDept);

            seedTeam("Manual QA Team", "Manual Testing", qaDept);
            seedTeam("Automation QA Team", "Selenium, Cypress", qaDept);

            seedTeam("IT Recruitment Team", "Tech Hiring", recruitDept);
            seedTeam("Mass Recruitment Team", "Non-tech Hiring", recruitDept);

            seedTeam("Internal Events Team", "Company Culture", erDept);
            seedTeam("C&B Team", "Compensation and Benefits", erDept);

            Position bePos = seedPosition("Backend Developer");
            Position fePos = seedPosition("Frontend Developer");
            Position qaPos = seedPosition("QA Engineer");
            Position pmPos = seedPosition("Project Manager");

            seedSkill("Java", bePos);
            seedSkill("Spring Boot", bePos);
            seedSkill("SQL", bePos);
            seedSkill("Redis", bePos);
            seedSkill("Docker", bePos);

            seedSkill("JavaScript", fePos);
            seedSkill("ReactJS", fePos);
            seedSkill("HTML/CSS", fePos);
            seedSkill("TypeScript", fePos);
            seedSkill("Redux", fePos);

            seedSkill("Manual Testing", qaPos);
            seedSkill("Automation Testing", qaPos);
            seedSkill("Selenium", qaPos);
            seedSkill("Postman", qaPos);
            seedSkill("Jira", qaPos);

            seedSkill("Agile", pmPos);
            seedSkill("Scrum", pmPos);
            seedSkill("Communication", pmPos);
            seedSkill("Risk Management", pmPos);
            seedSkill("Team Leadership", pmPos);

            // ========== 2. Seed Users with Department & Position ==========
            seedUser("admin@skillmatrix.com", "System Admin", "ADMIN", null, null);

            seedUser("manager_career@skillmatrix.com", "Manager Career 1", "MANAGER_CAREER", null, null);
            seedUser("manager_career2@skillmatrix.com", "Manager Career 2", "MANAGER_CAREER", null, null);

            seedUser("manager_department@skillmatrix.com", "Manager Department 1", "MANAGER_DEPARTMENT", devDept, pmPos);
            seedUser("manager_department2@skillmatrix.com", "Manager Department 2", "MANAGER_DEPARTMENT", qaDept, qaPos);

            seedUser("manager_team@skillmatrix.com", "Manager Team 1", "MANAGER_TEAM", devDept, bePos);
            seedUser("manager_team2@skillmatrix.com", "Manager Team 2", "MANAGER_TEAM", qaDept, qaPos);

            seedUser("member1@skillmatrix.com", "Team Member One", "STAFF", devDept, bePos);
            seedUser("member2@skillmatrix.com", "Team Member Two", "STAFF", devDept, bePos);
            seedUser("member3@skillmatrix.com", "Team Member Three", "STAFF", devDept, bePos);
            seedUser("member4@skillmatrix.com", "Team Member Four", "STAFF", devDept, fePos);
            seedUser("member5@skillmatrix.com", "Team Member Five", "STAFF", devDept, fePos);

            seedUser("member6@skillmatrix.com", "Team Member Six", "STAFF", qaDept, qaPos);
            seedUser("member7@skillmatrix.com", "Team Member Seven", "STAFF", qaDept, qaPos);
            seedUser("member8@skillmatrix.com", "Team Member Eight", "STAFF", qaDept, qaPos);
            seedUser("member9@skillmatrix.com", "Team Member Nine", "STAFF", recruitDept, null);
            seedUser("member10@skillmatrix.com", "Team Member Ten", "STAFF", erDept, null);
        };
    }

    // ========== Helper Methods ==========

    private void seedUser(String email, String fullName, String role, Department dept, Position position) {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("123456"));
            user.setFullName(fullName);
            user.setRole(role);
            user.setDepartment(dept);
            user.setPosition(position);
            user.setStatus(GeneralStatus.ACTIVE);
            userRepository.save(user);
        }
    }

    private Career seedCareer(String name, String desc, String type) {
        return careerRepository.findAll().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Career c = new Career();
                    c.setName(name);
                    c.setCareerType(type);
                    c.setDescription(desc);
                    c.setStatus(GeneralStatus.ACTIVE);
                    return careerRepository.save(c);
                });
    }

    private Department seedDepartment(String name, String desc, Career career) {
        return departmentRepository.findAll().stream()
                .filter(d -> name.equals(d.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setName(name);
                    d.setDescription(desc);
                    d.setCareer(career);
                    d.setStatus(GeneralStatus.ACTIVE);
                    return departmentRepository.save(d);
                });
    }

    private Team seedTeam(String name, String desc, Department dept) {
        return teamRepository.findAll().stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Team t = new Team();
                    t.setName(name);
                    t.setDescription(desc);
                    t.setDepartment(dept);
                    t.setStatus(GeneralStatus.ACTIVE);
                    return teamRepository.save(t);
                });
    }

    private Position seedPosition(String name) {
        return positionRepository.findAll().stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Position p = new Position();
                    p.setName(name);
                    p.setDescription(name + " Role");
                    return positionRepository.save(p);
                });
    }

    private void seedSkill(String name, Position pos) {
        Skill skill = skillRepository.findByNameIgnoreCase(name);
        if (skill == null) {
            skill = new Skill();
            skill.setName(name);
            skill.setDescription(name + " Skill");
            skill.setStatus(SkillStatus.ACTIVE);
            skill = skillRepository.save(skill);
        }
        final Long skillId = skill.getSkillId();
        boolean alreadyLinked = pos.getPositionSkills() != null
                && pos.getPositionSkills().stream()
                        .anyMatch(ps -> ps.getSkill().getSkillId().equals(skillId));
        if (!alreadyLinked) {
            PositionSkill ps = new PositionSkill();
            ps.setPosition(pos);
            ps.setSkill(skill);
            positionSkillRepository.save(ps);
        }
    }
}
