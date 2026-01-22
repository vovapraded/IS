package org.example.domain.import_history.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.ZonedDateTime;

@Entity
@Table(name = "import_operations")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ImportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @NotBlank
    @Column(name = "username", nullable = false)
    private String username; // Пользователь, который запустил операцию

    @NotBlank
    @Column(name = "filename", nullable = false)
    private String filename; // Имя загруженного файла

    @Column(name = "total_records")
    private Integer totalRecords; // Общее количество записей в файле

    @Column(name = "processed_records")
    private Integer processedRecords; // Количество обработанных записей

    @Column(name = "successful_records")
    private Integer successfulRecords; // Количество успешно добавленных записей

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // Сообщение об ошибке (если есть)

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = ZonedDateTime.now();
        }
        if (status == null) {
            status = ImportStatus.IN_PROGRESS;
        }
    }
}