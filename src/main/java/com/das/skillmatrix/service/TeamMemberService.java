package com.das.skillmatrix.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.das.skillmatrix.annotation.LogActivity;
import com.das.skillmatrix.dto.request.AddMemberByTeamRequest;
import com.das.skillmatrix.dto.request.AddMemberByUserRequest;
import com.das.skillmatrix.dto.request.EditMemberRequest;
import com.das.skillmatrix.dto.response.PageResponse;
import com.das.skillmatrix.dto.response.TeamMemberResponse;
import com.das.skillmatrix.entity.GeneralStatus;
import com.das.skillmatrix.entity.Position;
import com.das.skillmatrix.entity.Team;
import com.das.skillmatrix.entity.TeamMember;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.PositionRepository;
import com.das.skillmatrix.repository.TeamMemberRepository;
import com.das.skillmatrix.repository.TeamRepository;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final BusinessChangeLogService businessChangeLogService;

    @LogActivity(action = "ADD_TEAM_MEMBER_BY_USER", entityType = "TEAM_MEMBER")
    public List<TeamMemberResponse> addByUser(AddMemberByUserRequest req) {
        User user = getActiveUser(req.getUserId());
        List<Long> teamIds = req.getAssignments().stream().map(AddMemberByUserRequest.Assignment::getTeamId).toList();
        List<Long> positionIds = req.getAssignments().stream().map(AddMemberByUserRequest.Assignment::getPositionId).toList();
        Map<Long, Team> teamMap = teamRepository.findAllById(teamIds).stream().collect(Collectors.toMap(Team::getTeamId, t -> t));
        Map<Long, Position> positionMap = positionRepository.findAllById(positionIds).stream().collect(Collectors.toMap(Position::getPositionId, p -> p));
        List<TeamMember> membersToSave = new ArrayList<>();

        for (var assignment : req.getAssignments()) {
            Team team = teamMap.get(assignment.getTeamId());
            if (team == null) throw new IllegalArgumentException("TEAM_NOT_FOUND");
            if (team.getStatus() != GeneralStatus.ACTIVE) throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
            Position position = positionMap.get(assignment.getPositionId());
            if (position == null) throw new IllegalArgumentException("POSITION_NOT_FOUND");
            validateSameCareer(user.getUserId(), team.getTeamId());
            validateNotDuplicate(team.getTeamId(), user.getUserId());
            TeamMember member = new TeamMember();
            member.setTeam(team);
            member.setUser(user);
            member.setPosition(position);
            membersToSave.add(member);
        }
        teamMemberRepository.saveAll(membersToSave);
        return membersToSave.stream().map(this::toResponse).toList();
    }

    @LogActivity(action = "ADD_TEAM_MEMBER_BY_TEAM", entityType = "TEAM_MEMBER")
    public TeamMemberResponse addByTeam(AddMemberByTeamRequest req) {
        Team team = getActiveTeam(req.getTeamId());
        User user = userRepository.findUserByEmail(req.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("USER_NOT_FOUND");
        }
        if (user.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        validateSameCareer(user.getUserId(), team.getTeamId());
        validateNotDuplicate(team.getTeamId(), user.getUserId());
        Position position = getPosition(req.getPositionId());

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setPosition(position);
        teamMemberRepository.save(member);
        return toResponse(member);
    }

    @LogActivity(action = "EDIT_TEAM_MEMBER", entityType = "TEAM_MEMBER")
    public TeamMemberResponse update(Long id, EditMemberRequest req) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_MEMBER_NOT_FOUND"));
        if (member.getTeam().getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        if (member.getUser().getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        Position position = getPosition(req.getPositionId());
        
        Long oldPosId = member.getPosition() != null ? member.getPosition().getPositionId() : null;
        Long newPosId = position.getPositionId();
        
        member.setPosition(position);
        teamMemberRepository.save(member);
        
        if (oldPosId != null && !oldPosId.equals(newPosId)) {
            businessChangeLogService.log(
                    "CHANGE_TEAM_MEMBER_POSITION", "TEAM_MEMBER", id,
                    "positionId", oldPosId.toString(), newPosId.toString());
        }
        
        return toResponse(member);
    }

    @LogActivity(action = "DELETE_TEAM_MEMBER", entityType = "TEAM_MEMBER")
    public void delete(Long id) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_MEMBER_NOT_FOUND"));
        if (member.getTeam().getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        teamMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamMemberResponse> listByTeam(Long teamId, Pageable pageable) {
        var page = teamMemberRepository.findByTeam_TeamId(teamId, pageable);
        List<TeamMemberResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (user.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("USER_NOT_ACTIVE");
        }
        return user;
    }

    private Team getActiveTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("TEAM_NOT_FOUND"));
        if (team.getStatus() != GeneralStatus.ACTIVE) {
            throw new IllegalArgumentException("TEAM_NOT_ACTIVE");
        }
        return team;
    }

    private Position getPosition(Long positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("POSITION_NOT_FOUND"));
    }

    private void validateSameCareer(Long userId, Long teamId) {
        if (!userRepository.checkUserInSameCareerWithTeam(userId, teamId)) {
            throw new IllegalArgumentException("USER_NOT_IN_SAME_CAREER");
        }
    }

    private void validateNotDuplicate(Long teamId, Long userId) {
        if (teamMemberRepository.existsByTeam_TeamIdAndUser_UserId(teamId, userId)) {
            throw new IllegalArgumentException("USER_ALREADY_IN_TEAM");
        }
    }

    private TeamMemberResponse toResponse(TeamMember member) {
        return new TeamMemberResponse(
                member.getId(),
                member.getUser().getUserId(),
                member.getUser().getEmail(),
                member.getUser().getFullName(),
                member.getTeam().getTeamId(),
                member.getTeam().getName(),
                member.getPosition() != null ? member.getPosition().getPositionId() : null,
                member.getPosition() != null ? member.getPosition().getName() : null,
                member.getCreatedAt());
    }
}
