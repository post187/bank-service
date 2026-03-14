package com.example.Model.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user-profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id", updatable = false, nullable = false, unique = true)
    private Long userProfileId;

    private String firstName;
    private String lastName;

    private String gender;

    private String address;

    private String occupation;

    private String martialStatus;

    private String nationality;

}
