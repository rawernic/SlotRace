package de.slotrace.adapter.out.persistence;

import de.slotrace.domain.model.Laufstatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
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
@Table(name = "lauf")
class LaufEntity {

    @Id
    UUID id;
    int nummer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    Laufstatus status;

    Instant gestartetAm;
    Instant beendetAm;

    @ElementCollection
    @CollectionTable(name = "belegung", joinColumns = @JoinColumn(name = "lauf_id"))
    @OrderBy("spurNummer")
    List<BelegungEmbeddable> belegungen = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "lauf_id", nullable = false)
    @OrderBy("zeitpunkt")
    List<RundeEntity> runden = new ArrayList<>();

    @Embeddable
    static class BelegungEmbeddable {
        int spurNummer;
        UUID teilnehmerId;

        BelegungEmbeddable() {
        }

        BelegungEmbeddable(int spurNummer, UUID teilnehmerId) {
            this.spurNummer = spurNummer;
            this.teilnehmerId = teilnehmerId;
        }
    }
}
