package org.springboot.troubleticketapi.servicecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ServiceEntity {

    @Id
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false)
    private boolean active;
}
