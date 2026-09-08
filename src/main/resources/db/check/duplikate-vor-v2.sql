-- DB-002: Vor dem ersten Deployment mit Flyway (V2) auf der Prod-DB ausführen.
-- Liefert jede Abfrage 0 Zeilen, kann V2 die Unique-Constraints anlegen.
-- Treffer vorher über die UI oder per SQL bereinigen — sonst schlägt V2 fehl und die App startet nicht.
--
-- Ausführen: psql -U <user> -d <db> -f duplikate-vor-v2.sql
-- (liegt bewusst ausserhalb von db/migration, damit Flyway es nicht als Migration ausführt)

-- Mehrere Einladungen derselben Partei zum selben Event
SELECT event_id, partei_id, count(*) AS anzahl, array_agg(id) AS einladung_ids
FROM einladung GROUP BY event_id, partei_id HAVING count(*) > 1;

-- Mehrere Teilnahmen zur selben Einladung
SELECT einladung_id, count(*) AS anzahl, array_agg(id) AS teilnahme_ids
FROM teilnahme GROUP BY einladung_id HAVING count(*) > 1;

-- Mehrere Abrechnungen zur selben Teilnahme
SELECT teilnahme_id, count(*) AS anzahl, array_agg(id) AS abrechnung_ids
FROM abrechnung GROUP BY teilnahme_id HAVING count(*) > 1;

-- Beträge, die nicht in numeric(10,2) passen (> 99'999'999.99)
SELECT 'konsumationsangebot' AS tabelle, id, preis AS betrag FROM konsumationsangebot WHERE abs(preis) >= 1e8
UNION ALL SELECT 'allgemeinausgabe', id, betrag FROM allgemeinausgabe WHERE abs(betrag) >= 1e8
UNION ALL SELECT 'abrechnung', id, total_betrag FROM abrechnung WHERE abs(total_betrag) >= 1e8 OR abs(anteil_allgemeinkosten) >= 1e8 OR abs(total_konsumation) >= 1e8
UNION ALL SELECT 'zahlung', id, betrag FROM zahlung WHERE abs(betrag) >= 1e8;
