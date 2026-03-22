package com.example.Model.Entity;

import com.example.Model.Dto.Internal.Status.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false, unique = true)
    private Long userId;

    @Column
    private String email;

    @Column
    private String password;

    @Column
    private String contactNo;

    private boolean verifyEmail = false;

    private boolean enable = false;

    @CreationTimestamp
    private LocalDate creationOn;

    private Set<String> roles;

    @Enumerated(EnumType.STRING)
    private Status status;

    @CreationTimestamp
    private LocalDateTime lastLoginAt;

    private int loginAttempts;

    private LocalDateTime lockUntil;

    @CreationTimestamp
    private LocalDateTime lastChangePassword;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

}
