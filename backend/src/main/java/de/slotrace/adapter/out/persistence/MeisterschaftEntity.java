package de.slotrace.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meisterschaft")
class MeisterschaftEntity {

    @Id
    UUID id;
    String name;

    @ElementCollection
    @CollectionTable(name = "meisterschaft_punkte", joinColumns = @JoinColumn(name = "meisterschaft_id"))
    @OrderColumn(name = "position")
    @Column(name = "punkte")
    List<Integer> punkte = new ArrayList<>();
}
