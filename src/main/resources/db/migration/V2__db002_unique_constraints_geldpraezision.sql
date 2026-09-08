-- DB-002: Fachliche Kardinalitäten per DB-Constraint erzwingen und Geldbeträge explizit als numeric(10,2).
--
-- Läuft sowohl auf V1-Schemas (CI, frische DBs) als auch auf baselinten Hibernate-Schemas (Prod, Dev-DB).
-- Hibernate hat für die @OneToOne-Beziehungen bereits Unique-Constraints mit Hash-Namen angelegt
-- (z.B. ukh65pmqwuwtjs788nmtako74wk auf teilnahme.einladung_id) — diese werden auf sprechende Namen
-- umbenannt, fehlende werden ergänzt. Bei Duplikaten im Bestand schlägt das Skript fehl (DDL ist
-- transaktional): vorher db/check/duplikate-vor-v2.sql ausführen.

-- Unique-Constraint sicherstellen: vorhandenen (beliebig benannten) Unique-Constraint auf exakt
-- diesen Spalten umbenennen, sonst neu anlegen.
CREATE OR REPLACE FUNCTION pg_temp.ensure_unique(p_table text, p_name text, p_columns text[])
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_existing text;
BEGIN
    SELECT c.conname INTO v_existing
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    WHERE t.relname = p_table
      AND c.contype = 'u'
      AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
           FROM unnest(c.conkey) k JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k)
          = (SELECT array_agg(x ORDER BY x) FROM unnest(p_columns) x)
    LIMIT 1;

    IF v_existing IS NULL THEN
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I UNIQUE (%s)',
                       p_table, p_name, array_to_string(p_columns, ', '));
    ELSIF v_existing <> p_name THEN
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I', p_table, v_existing, p_name);
    END IF;
END $$;

-- Eine Einladung pro Event und Partei (UC-004 E1), Einladung 1—1 Teilnahme (UC-005), Teilnahme 1—1 Abrechnung (UC-011)
SELECT pg_temp.ensure_unique('einladung',  'uk_einladung_event_partei', ARRAY['event_id', 'partei_id']);
SELECT pg_temp.ensure_unique('teilnahme',  'uk_teilnahme_einladung',    ARRAY['einladung_id']);
SELECT pg_temp.ensure_unique('abrechnung', 'uk_abrechnung_teilnahme',   ARRAY['teilnahme_id']);
SELECT pg_temp.ensure_unique('benutzer',   'uk_benutzer_email',         ARRAY['email']);

-- Geldbeträge: bisher Hibernate-Default numeric(38,2), neu explizit numeric(10,2) (max. 99'999'999.99)
ALTER TABLE konsumationsangebot ALTER COLUMN preis TYPE numeric(10,2);
ALTER TABLE allgemeinausgabe    ALTER COLUMN betrag TYPE numeric(10,2);
ALTER TABLE abrechnung
    ALTER COLUMN anteil_allgemeinkosten TYPE numeric(10,2),
    ALTER COLUMN total_konsumation      TYPE numeric(10,2),
    ALTER COLUMN total_betrag           TYPE numeric(10,2);
ALTER TABLE zahlung             ALTER COLUMN betrag TYPE numeric(10,2);
