package de.slotrace.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bahn")
class BahnEntity {

    @Id
    UUID id;
    String name;

    @ElementCollection
    @CollectionTable(name = "spur", joinColumns = @JoinColumn(name = "bahn_id"))
    @OrderBy("nummer")
    List<SpurEmbeddable> spuren = new ArrayList<>();

    @Embeddable
    static class SpurEmbeddable {
        int nummer;
        String farbe;

        SpurEmbeddable() {
        }

        SpurEmbeddable(int nummer, String farbe) {
            this.nummer = nummer;
            this.farbe = farbe;
        }
    }
}
