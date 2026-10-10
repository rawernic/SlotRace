package de.slotrace.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runde")
class RundeEntity {

    @Id
    UUID id;
    UUID teilnehmerId;
    int spurNummer;
    int nummer;
    Instant zeitpunkt;
    long rundenzeitMs;
}
