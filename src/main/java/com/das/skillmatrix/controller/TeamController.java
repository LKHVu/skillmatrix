package com.das.skillmatrix.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.das.skillmatrix.dto.request.TeamRequest;
import com.das.skillmatrix.dto.response.ApiResponse;
import com.das.skillmatrix.dto.response.TeamResponse;
import com.das.skillmatrix.service.TeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class TeamController {
    private final TeamService teamService;
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/teams")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamRequest teamRequest) {
        TeamResponse teamResponse = teamService.createTeam(teamRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(teamResponse, true, null));
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        List<TeamResponse> teamResponses = teamService.getAllTeams();
        return ResponseEntity.ok(new ApiResponse<>(teamResponses, true, null));
    }
    
    @PreAuthorize("@auth.requireTeamManagerOwner(#teamId)")
    @PutMapping("/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@PathVariable Long teamId, @Valid @RequestBody TeamRequest teamRequest) {
        TeamResponse teamResponse = teamService.updateTeam(teamId, teamRequest);
        return ResponseEntity.ok(new ApiResponse<>(teamResponse, true, null));
    }

    @PreAuthorize("@auth.requireTeamManagerOwner(#teamId)")
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable Long teamId) {
        TeamResponse teamResponse = teamService.getTeamById(teamId);
        return ResponseEntity.ok(new ApiResponse<>(teamResponse, true, null));
    }

    @PreAuthorize("@auth.requireTeamManagerOwner(#teamId)")
    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
