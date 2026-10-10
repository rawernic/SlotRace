package de.slotrace.adapter.out.persistence;

import de.slotrace.domain.model.Rennmodus;
import de.slotrace.domain.model.Rennstatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rennen")
class RennenEntity {

    @Id
    UUID id;
    String name;
    UUID bahnId;
    UUID meisterschaftId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    Rennmodus modus;

    Integer laufDauerSekunden;
    Integer rundenZiel;
    int mindestRundenzeitMs;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    Rennstatus status;

    Instant erstelltAm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "rennen_id", nullable = false)
    @OrderBy("startnummer")
    List<TeilnehmerEntity> teilnehmer = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "rennen_id", nullable = false)
    @OrderBy("nummer")
    List<LaufEntity> laeufe = new ArrayList<>();
}
