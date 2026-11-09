package dollar_risk_box_v2.core;

import java.util.Locale;

/** The texts of a box's labels, as on Futures Tester's risk box (D-090), in English. No MotiveWave classes here. */
public final class Labels {

    private Labels() { }

    /** Whole dollars, as the tester shows them: "$1,234". */
    public static String usd(double v) {
        return "$" + String.format(Locale.US, "%,d", Math.round(Math.abs(v)));
    }

    /**
     * The three label texts {target, entry, stop} and the warning (or null), as on the tester's box: "Target +$351 ·
     * R:R 2.00", "Buy 9 MNQ · risk $194", "Stop -$194 (40 ticks)"; with prices on, each also says its price.
     */
    public static String[] texts(RiskSize.Result r, String root, boolean prices, java.util.function.DoubleFunction<String> fmt,
                               double entry, double stop, double target) {
        String side = r.buy() ? "Buy" : "Sell";
        String t = "Target " + (prices ? fmt.apply(target) + " \u00b7 " : "") + "+" + usd(r.reward())
                + " \u00b7 R:R " + String.format(Locale.US, "%.2f", r.rr());
        String e = side + " " + r.qty() + " " + root + (prices ? " @ " + fmt.apply(entry) : "") + " \u00b7 risk " + usd(r.risk());
        String st = "Stop " + (prices ? fmt.apply(stop) + " \u00b7 " : "") + "\u2212" + usd(r.risk())
                + " (" + r.stopTicks() + (r.stopTicks() == 1 ? " tick)" : " ticks)");
        String warn = r.overBudget() ? "1 contract costs more than the risk " + usd(r.budget())
                : r.capped() ? "limit: " + r.qty() + " contracts" : null;
        return new String[]{t, e, st, warn};
    }

}
