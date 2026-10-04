-- Optimistik kilitleme (version) kolonları: eşzamanlı güncelleme koruması.
--
-- Gerekçe: bu tablolarda @Version kolonu yoktu. İki eşzamanlı istek aynı kaydı
-- okuyup farklı değerlerle yazdığında "son yazan kazanır" (lost update) oluşuyordu.
-- Kritik örnekler:
--   * ik.maas_bordro  -> bordro iki kez ödenebiliyordu (gerçek para kaybı)
--   * stok.uretim_emri -> üretim emri iki kez çalışıyor, hammadde iki kez tüketiliyor,
--                        mamul iki kez üretiliyor (stok ve maliyet şişmesi)
--   * sistem.donem    -> dönem kilitleme/çözme yarış durumunda yanlış durumda kalıyor
--   * ticaret.teslimat-> iki şoför aynı anda "teslim edildi" işaretleyebiliyor,
--                        TeslimatDurumLog iki çelişkili satır alıyor
--   * muhasebe.muhasebe_fisi -> fiş durum geçişi (ONAYLANDI/IPTAL) kaybolabiliyor
--   * ticaret.teklif   -> eşzamanlı revizyon uk_teklif_no_sirket_rev ihlali yapıyor
--
-- Güvenli: yalnızca yeni kolon, NOT NULL DEFAULT 0. Mevcut satırlar 0 alır,
-- veri okunmaz/yazılmaz/deletelenmez. Geri alınabilir (DROP COLUMN).

ALTER TABLE stok.uretim_emri      ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ik.maas_bordro       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sistem.donem         ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ticaret.teslimat     ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE muhasebe.muhasebe_fisi ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ticaret.teklif       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;