package com.petspot.domain.favorite.entity;

import com.petspot.domain.place.entity.Place;
import com.petspot.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 장소 즐겨찾기 JPA 엔티티 (User와 Place간 N:M 해소를 위한 중간 매핑 Entity)
 */
@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorites_user_place", columnNames = {"user_id", "place_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Builder(access = AccessLevel.PROTECTED)
    protected Favorite(UUID id, User user, Place place) {
        this.id = id != null ? id : UUID.randomUUID();
        this.user = user;
        this.place = place;
    }

    /**
     * Favorite 정적 팩토리 메서드
     */
    public static Favorite create(User user, Place place) {
        return Favorite.builder()
                .user(user)
                .place(place)
                .build();
    }
}
