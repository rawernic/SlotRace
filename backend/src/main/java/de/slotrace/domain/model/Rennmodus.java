package de.slotrace.domain.model;

public enum Rennmodus {
    /** Wer in der vorgegebenen Laufzeit die meisten Runden fährt. */
    ZEITRENNEN,
    /** Wer die vorgegebene Rundenzahl am schnellsten fährt. */
    RUNDENRENNEN,
    /** Freies Fahren ohne Zeit- oder Rundenlimit; gewertet wird die schnellste Runde. */
    TRAINING
}
