package database;

import service.PriceTierService;
import service.impl.PriceTierServiceImpl;

public class TestDB {
    public static void main(String[] args) {
        PriceTierService service = new PriceTierServiceImpl();
        double total50 = service.calculateTotal(50);
        double total51 = service.calculateTotal(51);
        double expected51 = 50 * 1806 + 1 * 1866;
        System.out.println("Total for 50 kWh: " + total50 + " (expected: " + (50 * 1806) + ")");
        System.out.println("Total for 51 kWh: " + total51 + " (expected: " + expected51 + ")");
        if (total51 == expected51) {
            System.out.println("Calculation for 51 kWh is CORRECT!");
        } else {
            System.out.println("Calculation for 51 kWh is INCORRECT! Actual: " + total51);
        }
    }
}
