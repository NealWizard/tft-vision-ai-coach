/**
 * INTENTIONAL VIOLATION FIXTURE — used only to prove CI safety scanner fails.
 * This file is excluded from production scans via tools/safety-fixtures allowlist.
 * DO NOT copy these APIs into product modules.
 */
public class ForbiddenInputSimulation {
    public void bad() {
        // SendInput( ... )
        System.out.println("SendInput");
    }
}
