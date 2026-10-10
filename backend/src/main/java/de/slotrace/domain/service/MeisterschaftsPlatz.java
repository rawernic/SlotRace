package de.slotrace.domain.service;

import java.util.UUID;

public record MeisterschaftsPlatz(int platz, UUID fahrerId, int punkte, int siege, int rennen) {
}
