package com.raspel.erp.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * İstemci gerçek IP adresini çözer. Ters proxy (Traefik) arkasında
 * {@code getRemoteAddr()} proxy adresini döndürdüğü için denetim izlerinde
 * yanlış IP kaydedilir; X-Forwarded-For yalnızca güvenilir (özel/loopback)
 * proxy'den geldiğinde dikkate alınır.
 */
public final class IstekYardimci {

    private IstekYardimci() {
    }

    public static String istemciIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        String remote = req.getRemoteAddr();
        if (xff != null && !xff.isBlank() && guvenilirProxyMu(remote)) {
            return xff.split(",")[0].trim();
        }
        return remote;
    }

    /** X-Forwarded-For yalnızca özel/loopback ağdan gelen proxy'de güvenilirdir. */
    private static boolean guvenilirProxyMu(String addr) {
        if (addr == null) return false;
        if (addr.equals("127.0.0.1") || addr.equals("::1") || addr.equals("0:0:0:0:0:0:0:1")) return true;
        if (addr.startsWith("10.")) return true;
        if (addr.startsWith("192.168.")) return true;
        if (addr.startsWith("172.")) {
            try {
                int ikinci = Integer.parseInt(addr.substring(4, addr.indexOf('.', 4)));
                return ikinci >= 16 && ikinci <= 31;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
