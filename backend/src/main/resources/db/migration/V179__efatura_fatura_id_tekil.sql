-- Ayni faturaya bagli mukerrer e-fatura kaydi olusmasini engelle.
-- Once olasi mukerrerlerden en eskisini birak, sonra tekil indeksi kur.
DELETE FROM fatura.e_fatura e
WHERE e.id NOT IN (
    SELECT MIN(ic.id)
    FROM fatura.e_fatura ic
    GROUP BY ic.fatura_id
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_efatura_fatura_id
    ON fatura.e_fatura (fatura_id);
