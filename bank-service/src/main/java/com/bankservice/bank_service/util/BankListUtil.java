package com.bankservice.bank_service.util;

import java.util.List;
import java.util.Map;

public class BankListUtil {
    public static List<Map<String, String>> getBanks() {
        return List.of(
            Map.of("code", "MIBANK", "name", "Mi Banco"),
            Map.of("code", "BCP", "name", "Banco de Crédito del Perú"),
            Map.of("code", "BBVA", "name", "BBVA Perú"),
            Map.of("code", "SCOTIABANK", "name", "Scotiabank Perú"),
            Map.of("code", "INTERBANK", "name", "Interbank"),
            Map.of("code", "BANBIF", "name", "BanBif")
        );
    }
}
