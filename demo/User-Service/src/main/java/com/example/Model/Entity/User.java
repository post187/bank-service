package com.example.Model.Entity;

import com.example.Model.Dto.Internal.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
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

    @Column
    private String authId;

    private boolean verifyEmail = false;

    private boolean enable = false;

    private String identificationNumber;

    @CreationTimestamp
    private LocalDate creationOn;

    private Set<String> roles;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;
}
