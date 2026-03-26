package com.das.skillmatrix.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.das.skillmatrix.dto.request.AddMemberByTeamRequest;
import com.das.skillmatrix.dto.request.AddMemberByUserRequest;
import com.das.skillmatrix.dto.request.EditMemberRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamMemberResponse;
import com.das.skillmatrix.entity.Career;
import com.das.skillmatrix.entity.Department;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Position;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.PositionRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeamMemberServiceTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private BusinessChangeLogService businessChangeLogService;

    @InjectMocks
    private TeamMemberService teamMemberService;

    private Career career;
    private Department department;
    private Team activeTeam;
    private Team activeTeam2;
    private User activeUser;
    private Position position1;
    private Position position2;

    @BeforeEach
    void setUp() {
        career = new Career();
        career.setCareerId(1L);
        career.setName("IT Career");

        department = new Department();
        department.setDepartmentId(1L);
        department.setName("Software Development");
        department.setStatus(GeneralStatus.ACTIVE);
        department.setCareer(career);

        activeTeam = team(1L, "Backend Team", department);
        activeTeam2 = team(2L, "Frontend Team", department);
        activeUser = user(8L, "member1@skillmatrix.com", "Team Member One", department);
        position1 = position(1L, "Backend Developer");
        position2 = position(2L, "Frontend Developer");
    }

    private static Team team(Long id, String name, Department dept) {
        Team t = new Team();
        t.setTeamId(id);
        t.setName(name);
        t.setDescription("desc");
        t.setStatus(GeneralStatus.ACTIVE);
        t.setCreatedAt(LocalDateTime.now());
        t.setDepartment(dept);
        t.setManagers(new ArrayList<>());
        return t;
    }

    private static User user(Long id, String email, String fullName, Department dept) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setRole("STAFF");
        u.setStatus(GeneralStatus.ACTIVE);
        u.setDepartment(dept);
        u.setManagedCareers(new ArrayList<>());
        u.setManagedDepartments(new ArrayList<>());
        u.setManagedTeams(new ArrayList<>());
        return u;
    }

    private static Position position(Long id, String name) {
        Position p = new Position();
        p.setPositionId(id);
        p.setName(name);
        p.setDescription(name + " Role");
        return p;
    }

    private static TeamMember teamMember(Long id, Team team, User user, Position position) {
        TeamMember m = new TeamMember();
        m.setId(id);
        m.setTeam(team);
        m.setUser(user);
        m.setPosition(position);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    // ==================== ADD BY USER ====================

    @Test
    @DisplayName("addByUser() should create multiple team members in batch")
    void addByUser_success() {
        var a1 = new AddMemberByUserRequest.Assignment(1L, 1L);
        var a2 = new AddMemberByUserRequest.Assignment(2L, 2L);
        var req = new AddMemberByUserRequest(8L, List.of(a1, a2));

        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser));
        when(teamRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(activeTeam, activeTeam2));
        when(positionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(position1, position2));
        when(userRepository.checkUserInSameCareerWithTeam(8L, 1L)).thenReturn(true);
        when(userRepository.checkUserInSameCareerWithTeam(8L, 2L)).thenReturn(true);
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(anyLong(), eq(8L))).thenReturn(false);
        when(teamMemberRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<TeamMember> members = inv.getArgument(0);
            long counter = 1;
            for (TeamMember m : members) {
                m.setId(counter++);
            }
            return members;
        });

        List<TeamMemberResponse> results = teamMemberService.addByUser(req);

        assertEquals(2, results.size());
        verify(teamMemberRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("addByUser() should throw when user not found")
    void addByUser_userNotFound() {
        var req = new AddMemberByUserRequest(999L,
                List.of(new AddMemberByUserRequest.Assignment(1L, 1L)));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByUser(req));
        assertEquals("USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("addByUser() should throw when team not found")
    void addByUser_teamNotFound() {
        var req = new AddMemberByUserRequest(8L,
                List.of(new AddMemberByUserRequest.Assignment(99L, 1L)));

        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser));
        when(teamRepository.findAllById(List.of(99L))).thenReturn(List.of());
        when(positionRepository.findAllById(List.of(1L))).thenReturn(List.of(position1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByUser(req));
        assertEquals("TEAM_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("addByUser() should throw when user not in same career")
    void addByUser_notSameCareer() {
        var req = new AddMemberByUserRequest(8L,
                List.of(new AddMemberByUserRequest.Assignment(1L, 1L)));

        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser));
        when(teamRepository.findAllById(List.of(1L))).thenReturn(List.of(activeTeam));
        when(positionRepository.findAllById(List.of(1L))).thenReturn(List.of(position1));
        when(userRepository.checkUserInSameCareerWithTeam(8L, 1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByUser(req));
        assertEquals("USER_NOT_IN_SAME_CAREER", ex.getMessage());
    }

    @Test
    @DisplayName("addByUser() should throw when user already in team")
    void addByUser_duplicate() {
        var req = new AddMemberByUserRequest(8L,
                List.of(new AddMemberByUserRequest.Assignment(1L, 1L)));

        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser));
        when(teamRepository.findAllById(List.of(1L))).thenReturn(List.of(activeTeam));
        when(positionRepository.findAllById(List.of(1L))).thenReturn(List.of(position1));
        when(userRepository.checkUserInSameCareerWithTeam(8L, 1L)).thenReturn(true);
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(1L, 8L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByUser(req));
        assertEquals("USER_ALREADY_IN_TEAM", ex.getMessage());
    }

    // ==================== ADD BY TEAM ====================

    @Test
    @DisplayName("addByTeam() should add user by email")
    void addByTeam_success() {
        var req = new AddMemberByTeamRequest(1L, "member1@skillmatrix.com", 1L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(userRepository.findUserByEmail("member1@skillmatrix.com")).thenReturn(activeUser);
        when(userRepository.checkUserInSameCareerWithTeam(8L, 1L)).thenReturn(true);
        when(teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(1L, 8L)).thenReturn(false);
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position1));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> {
            TeamMember saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TeamMemberResponse res = teamMemberService.addByTeam(req);

        assertNotNull(res);
        assertEquals("member1@skillmatrix.com", res.getEmail());
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("addByTeam() should throw when user not found by email")
    void addByTeam_userNotFound() {
        var req = new AddMemberByTeamRequest(1L, "unknown@skillmatrix.com", 1L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));
        when(userRepository.findUserByEmail("unknown@skillmatrix.com")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByTeam(req));
        assertEquals("USER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("addByTeam() should throw when team not active")
    void addByTeam_teamNotActive() {
        activeTeam.setStatus(GeneralStatus.DEACTIVE);
        var req = new AddMemberByTeamRequest(1L, "member1@skillmatrix.com", 1L);

        when(teamRepository.findById(1L)).thenReturn(Optional.of(activeTeam));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.addByTeam(req));
        assertEquals("TEAM_NOT_ACTIVE", ex.getMessage());
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("update() should change position and log")
    void update_success() {
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionRepository.findById(2L)).thenReturn(Optional.of(position2));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamMemberResponse res = teamMemberService.update(1L, new EditMemberRequest(2L));

        assertEquals("Frontend Developer", res.getPositionName());
        verify(businessChangeLogService).log(
                eq("CHANGE_TEAM_MEMBER_POSITION"), eq("TEAM_MEMBER"), eq(1L),
                eq("positionId"), eq("1"), eq("2"));
    }

    @Test
    @DisplayName("update() should not log when position unchanged")
    void update_samePosition() {
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position1));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        teamMemberService.update(1L, new EditMemberRequest(1L));

        verify(businessChangeLogService, never()).log(anyString(), anyString(), anyLong(),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("update() should throw when member not found")
    void update_notFound() {
        when(teamMemberRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.update(99L, new EditMemberRequest(1L)));
        assertEquals("TEAM_MEMBER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("update() should throw when team not active")
    void update_teamNotActive() {
        activeTeam.setStatus(GeneralStatus.DEACTIVE);
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.update(1L, new EditMemberRequest(2L)));
        assertEquals("TEAM_NOT_ACTIVE", ex.getMessage());
    }

    @Test
    @DisplayName("update() should throw when user not active")
    void update_userNotActive() {
        activeUser.setStatus(GeneralStatus.DEACTIVE);
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.update(1L, new EditMemberRequest(2L)));
        assertEquals("USER_NOT_ACTIVE", ex.getMessage());
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("delete() should hard delete member")
    void delete_success() {
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));

        teamMemberService.delete(1L);

        verify(teamMemberRepository).delete(existing);
    }

    @Test
    @DisplayName("delete() should throw when member not found")
    void delete_notFound() {
        when(teamMemberRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.delete(99L));
        assertEquals("TEAM_MEMBER_NOT_FOUND", ex.getMessage());
    }

    @Test
    @DisplayName("delete() should throw when team not active")
    void delete_teamNotActive() {
        activeTeam.setStatus(GeneralStatus.DEACTIVE);
        TeamMember existing = teamMember(1L, activeTeam, activeUser, position1);

        when(teamMemberRepository.findById(1L)).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> teamMemberService.delete(1L));
        assertEquals("TEAM_NOT_ACTIVE", ex.getMessage());
    }

    // ==================== LIST BY TEAM ====================

    @Test
    @DisplayName("listByTeam() should return paginated members")
    void listByTeam_success() {
        Pageable pageable = PageRequest.of(0, 10);
        TeamMember m1 = teamMember(1L, activeTeam, activeUser, position1);

        User user2 = user(9L, "member2@skillmatrix.com", "Team Member Two", department);
        TeamMember m2 = teamMember(2L, activeTeam, user2, position1);

        when(teamMemberRepository.findByTeam_TeamId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(m1, m2), pageable, 2));

        PageResponse<TeamMemberResponse> res = teamMemberService.listByTeam(1L, pageable);

        assertEquals(2, res.getItems().size());
        assertEquals(2L, res.getTotalElements());
        assertEquals(1, res.getTotalPages());
        assertEquals("member1@skillmatrix.com", res.getItems().get(0).getEmail());
        assertEquals("member2@skillmatrix.com", res.getItems().get(1).getEmail());
    }

    @Test
    @DisplayName("listByTeam() should return empty when no members")
    void listByTeam_empty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(teamMemberRepository.findByTeam_TeamId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<TeamMemberResponse> res = teamMemberService.listByTeam(1L, pageable);

        assertTrue(res.getItems().isEmpty());
        assertEquals(0L, res.getTotalElements());
    }
}
