package dollar_risk_box_v2;

import dollar_risk_box_v2.core.Costs;
import dollar_risk_box_v2.core.RiskSize;
import dollar_risk_box_v2.core.RiskSize.Budget;
import dollar_risk_box_v2.core.RiskSize.Spec;

/** Plain-Java tests (no framework): run by build.sh; any failure stops the build. */
public final class Tests {
    private static int passed, failed;

    public static void main(String[] a) {
        likeTheTester();
        commissions();
        labels();
        System.out.println("tests: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(boolean ok, String what) { if (ok) passed++; else { failed++; System.out.println("FAIL: " + what); } }
    private static boolean near(double x, double y) { return Math.abs(x - y) < 1e-9; }

    private static void labels() {
        var r = RiskSize.of(20000, 19990, 20020, MNQ, new Budget(false, 200), 0);
        String[] t = dollar_risk_box_v2.core.Labels.texts(r, "MNQ", false, v -> String.format(java.util.Locale.US, "%.2f", v), 20000, 19990, 20020);
        check(t[0].equals("Target +$351 \u00b7 R:R 2.00"), "target label: " + t[0]);
        check(t[1].equals("Buy 9 MNQ \u00b7 risk $194"), "entry label: " + t[1]);
        check(t[2].equals("Stop \u2212$194 (40 ticks)"), "stop label: " + t[2]);
        check(t[3] == null, "no warning");
        String[] p = dollar_risk_box_v2.core.Labels.texts(r, "MNQ", true, v -> String.format(java.util.Locale.US, "%.2f", v), 20000, 19990, 20020);
        check(p[0].startsWith("Target 20020.00") && p[1].contains("@ 20000.00") && p[2].startsWith("Stop 19990.00"), "with prices");
        var o = RiskSize.of(20000, 19900, 20100, MNQ, new Budget(false, 100), 0);
        check("1 contract costs more than the risk $100".equals(dollar_risk_box_v2.core.Labels.texts(o, "MNQ", false, v -> "", 20000, 19900, 20100)[3]), "over budget warning");
        var c = RiskSize.of(20000, 19999.75, 20001, MNQ, new Budget(false, 5000), 0);
        check("limit: 40 contracts".equals(dollar_risk_box_v2.core.Labels.texts(c, "MNQ", false, v -> "", 20000, 19999.75, 20001)[3]), "limit warning");
        check(dollar_risk_box_v2.core.Labels.texts(c, "MNQ", false, v -> "", 20000, 19999.75, 20001)[2].endsWith("(1 tick)"), "1 tick");
        var s = RiskSize.of(20000, 20010, 19980, MNQ, new Budget(false, 200), 0);
        check(dollar_risk_box_v2.core.Labels.texts(s, "MNQ", false, v -> "", 20000, 20010, 19980)[1].startsWith("Sell 9 MNQ"), "sell label");
        check(dollar_risk_box_v2.core.Labels.usd(1234.5).equals("$1,235") && dollar_risk_box_v2.core.Labels.usd(-193.5).equals("$194"), "whole dollars");
    }

    private static void commissions() {
        check(Costs.root("MNQZ6").equals("MNQ") && Costs.root("MGCZ6").equals("MGC") && Costs.root("6EZ6").equals("6E")
                && Costs.root("M2KZ6").equals("M2K") && Costs.root("SILZ6").equals("SIL") && Costs.root("ESZ26").equals("ES")
                && Costs.root("NQZ6.CME.RITHMIC").equals("NQ") && Costs.root("QMZ6").equals("QM"), "contract roots");
        String lucid = "Lucid Trading (Frankfurt)";
        var a = Costs.of(Costs.Mode.AUTO, lucid, "MNQZ6", 0.3);
        check(near(a.perSide(), 0.50) && a.source().equals("Lucid"), "Lucid MNQ 0.50");
        check(near(Costs.of(Costs.Mode.AUTO, lucid, "MGCZ6", 0.3).perSide(), 0.80), "Lucid MGC 0.80");
        check(near(Costs.of(Costs.Mode.AUTO, lucid, "NQZ6", 0.3).perSide(), 1.75), "Lucid NQ 1.75");
        var unknown = Costs.of(Costs.Mode.AUTO, lucid, "BTCZ6", 0.3);
        check(near(unknown.perSide(), 0.3) && unknown.source().startsWith("manual"), "not in Lucid's list: the typed value");
        check(near(Costs.of(Costs.Mode.AUTO, "Rithmic Paper Trading", "MNQZ6", 0.25).perSide(), 0.25), "another server: typed");
        check(near(Costs.of(Costs.Mode.AUTO, null, "MNQZ6", 0.25).perSide(), 0.25), "unknown server: typed");
        check(near(Costs.of(Costs.Mode.MANUAL, lucid, "MNQZ6", 0.25).perSide(), 0.25), "Manual chosen: typed even on Lucid");
        check(near(Costs.of(Costs.Mode.MANUAL, lucid, "MNQZ6", -1).perSide(), 0), "negative typed: 0");
        String log = "18:42:15 INFO Loading Workspace\n18:42:17.377 INFO RithmicService::connect() Connecting to server: Lucid Trading (Frankfurt) username: LT-X test: false\n"
                + "19:00 INFO RithmicService::connect() Connecting to server: Rithmic Test username: Y test: true\n";
        check("Rithmic Test".equals(Costs.lastServer(log)), "the LAST connection counts");
        check(Costs.lastServer("nothing here") == null, "no connection line: null");
        check(near(Costs.of(Costs.Mode.LUCID, null, "MGCZ6", 0.25).perSide(), 0.80), "Lucid chosen: the table even without a log");
        check(Costs.Mode.parse("nonsense") == Costs.Mode.AUTO && Costs.Mode.parse("LUCID") == Costs.Mode.LUCID, "mode parse");
    }

    // MNQ: tick 0.25 = $0.50; the tester's defaults: 1 tick of slippage, $0.50 per side, 40 contracts
    private static final Spec MNQ = new Spec(0.25, 0.5, 1, 0.5, 50_000, 40);

    private static void likeTheTester() {
        // buy: entry 20000, stop 19990 (40 ticks), target 20020 (80 ticks), $200
        var r = RiskSize.of(20000, 19990, 20020, MNQ, new Budget(false, 200), 0);
        check(r.valid() && r.buy(), "stop below entry = buy");
        check(r.stopTicks() == 40 && r.targetTicks() == 80, "ticks");
        check(near(r.perContract(), (40 + 1) * 0.5 + 2 * 0.5), "1R = (stop + slippage) x tick value + 2 commissions = 21.5");
        check(r.qty() == 9, "200 / 21.5 = 9.3 -> 9 contracts");
        check(near(r.risk(), 21.5 * 9), "risk = 1R x qty");
        check(near(r.reward(), (80 * 0.5 - 1.0) * 9), "target net of commissions");
        check(near(r.rr(), 2.0), "R:R by ticks");
        check(!r.overBudget() && !r.capped(), "no flags");
        // sell, mirrored
        var s = RiskSize.of(20000, 20010, 19980, MNQ, new Budget(false, 200), 0);
        check(s.valid() && !s.buy() && s.qty() == 9, "stop above entry = sell, same size");
        // percent of the balance: 0.4 % of 50 000 = 200
        check(RiskSize.of(20000, 19990, 20020, MNQ, new Budget(true, 0.4), 0).qty() == 9, "percent budget");
        // one contract costs more than the budget: still 1, flagged
        var o = RiskSize.of(20000, 19900, 20100, MNQ, new Budget(false, 100), 0);
        check(o.qty() == 1 && o.overBudget(), "at least 1 contract, flagged");
        // capped at the account limit
        var c = RiskSize.of(20000, 19999.75, 20001, MNQ, new Budget(false, 5000), 0);
        check(c.qty() == 40 && c.capped(), "at most maxQty, flagged");
        // wrong side / too close: not valid
        check(!RiskSize.of(20000, 19990, 19995, MNQ, new Budget(false, 200), 0).valid(), "target on the stop's side: invalid");
        check(!RiskSize.of(20000, 20000, 20020, MNQ, new Budget(false, 200), 0).valid(), "stop on the entry: invalid");
        check(!RiskSize.of(20000, 19999.9, 20020, MNQ, new Budget(false, 200), 0).valid(), "stop less than a tick: invalid");
        // a zero or negative budget: 1 contract, over budget
        var z = RiskSize.of(20000, 19990, 20020, MNQ, new Budget(false, 0), 0);
        check(z.qty() == 1 && z.overBudget(), "no budget: 1 contract, flagged");
        // fixed quantity wins, no flags
        var f = RiskSize.of(20000, 19990, 20020, MNQ, new Budget(false, 200), 3);
        check(f.qty() == 3 && !f.overBudget() && !f.capped() && near(f.risk(), 21.5 * 3), "fixed quantity");
        // MGC: tick 0.1 = $1; float prices (4500.3f) still round to whole ticks
        var g = RiskSize.of(4500.3f, 4495.3f, 4510.3f, new Spec(0.1, 1.0, 1, 0.5, 50_000, 40), new Budget(false, 200), 0);
        check(g.valid() && g.stopTicks() == 50 && g.targetTicks() == 100, "MGC ticks from float prices");
        check(near(g.perContract(), 52.0) && g.qty() == 3, "MGC 1R = 51 x $1 + $1 = 52 -> 3 contracts");
        // exact fit: 1e-9 guard keeps 215/21.5 = 10
        check(RiskSize.of(20000, 19990, 20020, MNQ, new Budget(false, 215), 0).qty() == 10, "exact fit is 10, not 9");
    }
}
