package com.example.Controller;

import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Response.CreateResponse;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<CreateResponse> createUser(@RequestBody CreateUser userDto) {
        log.info("Creating user with: {}", userDto.toString());
        return ResponseEntity.ok(userService.createUser(userDto));
    }

    @PatchMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateUserUpdate(@PathVariable Long id, @RequestBody UpdateStatus update) {
        log.info("updating the user with: {}", update.toString());
        return new ResponseEntity<>(userService.updateUserStatus(id, update), HttpStatus.OK);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> readUserById(@PathVariable Long userId) {
        log.info("reading user by ID");

        return ResponseEntity.ok(userService.readUserById(userId));
    }

    @PutMapping("/change-contact")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> changeContact(@RequestParam("email") String email, @RequestBody String contactNumber) {
        return ResponseEntity.ok(userService.changeContactNumber(email, contactNumber));
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> updateMyProfile(@RequestParam("email") String email, @RequestBody UpdateUserProfile userProfile) {
        return ResponseEntity.ok(userService.changeUserProfile(email, userProfile));
    }

//    @GetMapping("/account/{accountId}")
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    public ResponseEntity<UserDto> readUserByAccountId(@PathVariable String accountId) {
//        return ResponseEntity.ok(userService.readUserByAccountId(accountId));
//    }

    @PostMapping("/send-code")
    public ResponseEntity<Response> sendCode(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.sendCode(email));
    }

    @PostMapping("/verify-account")
    public ResponseEntity<Response> verifyAccount(@RequestParam("token") String token) {
        Response response = userService.verifyToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getUsers(@RequestParam("page") int page) {
        return ResponseEntity.ok(userService.readAllUsers(page));
    }

    @GetMapping("/my-info")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> getMyInformation(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.getMyInfo(email));
    }

    @PostMapping("login")
    public ResponseEntity<JwtResponse> login(@RequestBody UserLogin userLogin) {
        return ResponseEntity.ok(userService.login(userLogin));
    }

    @PutMapping("/change-profile/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateProfileUser(@PathVariable Long id, @RequestBody UpdateUserProfile userProfile) {
            return ResponseEntity.ok(userService.updateUserProfile(id, userProfile));
    }

    @PutMapping("/add-role-admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateRoleToUser(@PathVariable Long id, @RequestBody String role) {
        return ResponseEntity.ok(userService.addAdminRole(id));
    }

}
