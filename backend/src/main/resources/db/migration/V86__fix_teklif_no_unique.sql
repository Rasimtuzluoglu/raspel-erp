-- V86__fix_teklif_no_unique.sql
-- Sorun: V60, ticaret.teklif.teklif_no uzerinde GLOBAL unique index (uq_teklif_teklif_no)
-- olusturmustu. V53 ise (sirket_id, teklif_no) uzerinde sirket-bazli unique index
-- (uk_teklif_no_sirket) kurmustu. Global index, farkli sirketlerin ayni teklif
-- numarasini kullanmasini engelledigi icin multi-tenant kullanimda yanlis
-- kisit olusturuyordu (bir sirketin numarasi diger sirketi blokluyor).
-- Cozum: Global index kaldirilir; sirket-bazli benzersizlik korunur.
DROP INDEX IF EXISTS ticaret.uq_teklif_teklif_no;