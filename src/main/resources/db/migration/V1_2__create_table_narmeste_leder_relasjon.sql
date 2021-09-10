CREATE TABLE NARMESTE_LEDER_RELASJON
(
    id                           SERIAL               PRIMARY KEY,
    uuid                         VARCHAR(50)          NOT NULL UNIQUE,
    created_at                   timestamptz          NOT NULL,
    updated_at                   timestamptz          NOT NULL,
    referanse_uuid               VARCHAR(50)          NOT NULL UNIQUE,
    virksomhetsnummer            VARCHAR              NOT NULL,
    arbeidstaker_personident     VARCHAR              NOT NULL,
    narmeste_leder_personident   VARCHAR              NOT NULL,
    narmeste_leder_telefonnummer VARCHAR              NOT NULL,
    narmeste_leder_epost         VARCHAR              NOT NULL,
    arbeidsgiver_forskutterer    BOOLEAN,
    aktiv_fom                    DATE                 NOT NULL,
    aktiv_tom                    DATE,
    timestamp                    timestamptz          NOT NUll
);

CREATE INDEX IX_NL_RELASJON_AT_PERSONIDENT on NARMESTE_LEDER_RELASJON (arbeidstaker_personident);
CREATE INDEX IX_NL_RELASJON_NL_PERSONIDENT on NARMESTE_LEDER_RELASJON (narmeste_leder_personident);
