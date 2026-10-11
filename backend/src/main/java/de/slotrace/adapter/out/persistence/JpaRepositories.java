package de.slotrace.adapter.out.persistence;

import de.slotrace.domain.model.Rennstatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FahrerJpaRepository extends JpaRepository<FahrerEntity, UUID> {
}

interface FahrzeugJpaRepository extends JpaRepository<FahrzeugEntity, UUID> {
}

interface BahnJpaRepository extends JpaRepository<BahnEntity, UUID> {
}

interface MeisterschaftJpaRepository extends JpaRepository<MeisterschaftEntity, UUID> {
}

interface RennenJpaRepository extends JpaRepository<RennenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RennenEntity r where r.id = :id")
    Optional<RennenEntity> findByIdZumSchreiben(@Param("id") UUID id);

    List<RennenEntity> findByMeisterschaftId(UUID meisterschaftId);

    List<RennenEntity> findByBahnIdAndStatus(UUID bahnId, Rennstatus status);

    boolean existsByBahnId(UUID bahnId);

    boolean existsByMeisterschaftId(UUID meisterschaftId);

    @Query("select count(t) > 0 from TeilnehmerEntity t where t.fahrerId = :id")
    boolean existiertTeilnehmerMitFahrer(@Param("id") UUID fahrerId);

    @Query("select count(t) > 0 from TeilnehmerEntity t where t.fahrzeugId = :id")
    boolean existiertTeilnehmerMitFahrzeug(@Param("id") UUID fahrzeugId);
}
