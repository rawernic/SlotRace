package de.slotrace.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "teilnehmer")
class TeilnehmerEntity {

    @Id
    UUID id;
    UUID fahrerId;
    UUID fahrzeugId;
    int startnummer;
}
