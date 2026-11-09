package dollar_risk_box_v2.core;

/**
 * The size of a risk box exactly as Futures Tester computes it ({@code wwwroot/lab-logic.js riskBoxSize}, D-090):
 * <ul>
 *   <li>the side follows the stop: a stop below the entry is a buy, above it a sell; the target must lie on the other
 *       side; stop and target at least one tick from the entry, otherwise the box is not valid;</li>
 *   <li>one contract risks its 1R as the statistics count it: (stop in ticks + slippage ticks) x tick value + both
 *       commissions;</li>
 *   <li>contracts = what the budget buys at that 1R, at least 1 and at most the account's limit; the risk shown is 1R x
 *       contracts; the target is net of both commissions; R:R = target ticks / stop ticks.</li>
 * </ul>
 * No MotiveWave classes here: the study passes plain numbers.
 */
public final class RiskSize {

    private RiskSize() { }

    /** What the account and the instrument add: tick size and value, slippage on the stop, commission per side, balance and the contract limit. */
    public record Spec(double tick, double tickValue, int slippageTicks, double commission, double balance, int maxQty) {
        public Spec {
            if (!(tick > 0) || !(tickValue > 0) || slippageTicks < 0 || commission < 0 || maxQty < 1)
                throw new IllegalArgumentException("spec");
        }
    }

    /** The budget: a dollar amount, or a percent of the balance. */
    public record Budget(boolean percent, double value) { }

    public record Result(boolean valid, boolean buy, int stopTicks, int targetTicks, int qty, double perContract,
                         double budget, double risk, double reward, double rr, boolean overBudget, boolean capped) {
        static final Result INVALID = new Result(false, false, 0, 0, 0, 0, 0, 0, 0, 0, false, false);
    }

    /**
     * @param fixedQty a fixed number of contracts instead of the budget, or 0 to size from the budget
     */
    public static Result of(double entry, double stop, double target, Spec spec, Budget budget, int fixedQty) {
        int side = stop < entry && target > entry ? 1 : stop > entry && target < entry ? -1 : 0;
        if (side == 0) return Result.INVALID;
        int stopTicks = (int) Math.round((entry - stop) * side / spec.tick());
        int targetTicks = (int) Math.round((target - entry) * side / spec.tick());
        if (stopTicks < 1 || targetTicks < 1) return Result.INVALID;
        double perContract = (stopTicks + spec.slippageTicks()) * spec.tickValue() + 2 * spec.commission();
        double raw = budget.percent() ? spec.balance() * budget.value() / 100 : budget.value();
        double b = raw > 0 ? raw : 0;
        int fit = (int) Math.floor(b / perContract + 1e-9);
        int qty = fixedQty > 0 ? fixedQty : Math.min(spec.maxQty(), Math.max(1, fit));
        return new Result(true, side > 0, stopTicks, targetTicks, qty, perContract, b, perContract * qty,
                (targetTicks * spec.tickValue() - 2 * spec.commission()) * qty, (double) targetTicks / stopTicks,
                fixedQty <= 0 && fit < 1, fixedQty <= 0 && fit > spec.maxQty());
    }
}
