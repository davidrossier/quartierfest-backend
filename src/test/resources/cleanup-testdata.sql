-- Cleanup-Script für Testdaten in der quartierfest-Datenbank
-- Löscht alle Datensätze in FK-korrekter Reihenfolge (abhängige Tabellen zuerst)
-- Ausführen: psql -U qfuser -d quartierfest -f src/test/resources/cleanup-testdata.sql

DELETE FROM zahlung;
DELETE FROM mahnung;
DELETE FROM konsumation;
DELETE FROM abrechnung;
DELETE FROM konsumationsangebot;
DELETE FROM allgemeinausgabe;
DELETE FROM teilnahme;
DELETE FROM einladung;
DELETE FROM person;
DELETE FROM partei;
DELETE FROM event;
