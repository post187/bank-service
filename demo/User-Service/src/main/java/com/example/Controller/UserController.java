package com.example.Controller;

import com.example.Model.Dto.Internal.CreateUser;
import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UserDto;
import com.example.Model.Dto.Response.CreateResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("auth/{authId}")
    public ResponseEntity<UserDto> readUserByAuthId(@PathVariable String authId) {
        log.info("reading user by authId");
        return ResponseEntity.ok(userService.readUser(authId));
    }

    @PatchMapping("{id}")
    public ResponseEntity<Response> updateUserUpdate(@PathVariable Long id, @RequestBody UpdateStatus update) {
        log.info("updating the user with: {}", update.toString());
        return new ResponseEntity<>(userService.updateUserStatus(id, update), HttpStatus.OK);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> readUserById(@PathVariable Long userId) {
        log.info("reading user by ID");

        return ResponseEntity.ok(userService.readUserById(userId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<UserDto> readUserByAccountId(@PathVariable String accountId) {
        return ResponseEntity.ok(userService.readUserByAccountId(accountId));
    }

    @PostMapping("/send-code")
    public ResponseEntity<Response> sendCode(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.sendCode(email));
    }

    @PostMapping("/verify-account")
    public ResponseEntity<Response> verifyAccount(@RequestParam("token") String token) {
        Response response = userService.verifyToken(token);
        return ResponseEntity.ok(response);
    }
}
