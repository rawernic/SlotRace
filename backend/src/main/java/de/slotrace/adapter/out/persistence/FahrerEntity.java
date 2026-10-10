package de.slotrace.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "fahrer")
class FahrerEntity {

    @Id
    UUID id;
    String vorname;
    String nachname;
    String spitzname;
}
