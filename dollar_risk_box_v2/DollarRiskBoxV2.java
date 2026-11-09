package dollar_risk_box_v2;

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.DiscreteDescriptor;
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
import com.motivewave.platform.sdk.common.desc.EnabledDependency;
import com.motivewave.platform.sdk.common.desc.FontDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuDescriptor;
import com.motivewave.platform.sdk.common.menu.MenuItem;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.ResizePoint;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import dollar_risk_box_v2.core.Costs;
import dollar_risk_box_v2.core.Labels;
import dollar_risk_box_v2.core.RiskSize;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dollar Risk Box V2
 * ------------------
 * The risk box of the author's Futures Tester (D-090) on a MotiveWave chart: one tool, three clicks (entry, stop,
 * target); the side follows the stop (below the entry = buy); contracts from a $ or % risk at an honest 1R per
 * contract - (stop ticks + slippage ticks) x tick value + both commissions - at least 1 and at most a limit; the target
 * net of commissions; R:R by ticks. The arithmetic is in {@code core.RiskSize} and equals the tester's riskBoxSize.
 *
 * Display only: a plain Study, never a Strategy - it has no OrderContext and cannot send orders. V1 (Dollar Risk Box)
 * stays a separate indicator.
 *
 * The SDK has no API for custom drawing tools, so the study keeps its own on-chart panel: press "Risk box" (or R with
 * the mouse over the chart), then click entry, stop and target. Any number of boxes; drag their edges or the body.
 */
@StudyHeader(
        namespace = "com.zentrader",
        id = "DOLLAR_RISK_BOX_V2",
        rb = "dollar_risk_box_v2.nls.strings",
        name = "STUDY_NAME",
        desc = "STUDY_DESC",
        menu = "MENU_GENERAL",
        overlay = true,
        studyOverlay = true,
        requiresBarUpdates = true       // live P/L follows the last price
)
public class DollarRiskBoxV2 extends Study {

    // ==================== Settings keys ====================
    private static final String RISK_TYPE = "riskType";
    private static final String RISK_AMOUNT = "riskAmount";
    private static final String RISK_PERCENT = "riskPercent";
    private static final String BALANCE = "balance";
    private static final String FIXED_QTY_ENABLED = "fixedQtyEnabled";
    private static final String FIXED_QTY = "fixedQty";
    private static final String SHOW_PANEL = "showPanel";
    private static final String PLACE_MODE = "placeMode";
    private static final String BOX_WIDTH = "boxWidth";
    private static final String ESC_CANCELS = "escCancels";
    private static final String DEL_REMOVES = "delRemoves";
    private static final String ESC_DELETES = "escDeletes";
    private static final String KEEP_ON_TOP = "keepOnTop";
    private static final String HOTKEY_R = "hotkeyR";
    private static final String COMM_MODE = "commMode";
    private static final String COMMISSION = "commission";
    private static final String SLIPPAGE = "slippage";
    private static final String MAX_QTY = "maxQty";
    private static final String SHOW_PRICES = "showPrices";
    private static final String TARGET_COLOR = "targetColor";
    private static final String STOP_COLOR = "stopColor";
    private static final String ENTRY_COLOR = "entryColor";
    private static final String PLACE_ONE = "one", PLACE_THREE = "three";
    // Hidden state (no descriptor)
    private static final String BOXES = "boxes";             // "entry,stop,target,start,end;..."
    private static final String PANEL_FX = "panelFx";        // panel position as a fraction of the chart
    private static final String PANEL_FY = "panelFy";
    // Format
    private static final String TARGET_FILL = "targetFill";
    private static final String STOP_FILL = "stopFill";
    private static final String TARGET_LINE = "targetLine";
    private static final String ENTRY_LINE = "entryLine";
    private static final String STOP_LINE = "stopLine";
    private static final String FONT = "font";
    private static final String TARGET_FG = "targetFg";
    private static final String TARGET_BG = "targetBg";
    private static final String ENTRY_FG = "entryFg";
    private static final String ENTRY_BG = "entryBg";
    private static final String STOP_FG = "stopFg";
    private static final String STOP_BG = "stopBg";
    private static final String TEXT_ALIGN = "textAlign";
    private static final String SHOW_LABELS = "showLabels";
    private static final String SHOW_CURRENT_PNL = "showCurrentPnl";
    private static final String SHOW_PERCENT = "showPercent";
    private static final String SHOW_BALANCE = "showBalance";
    private static final String MIN_VISUAL = "minVisual";            // true = minimal labels: no prices
    private static final String LABEL_OPACITY = "labelOpacity";
    private static final String SHOW_QTY = "showQty";
    private static final String EXTEND_RIGHT = "extendRight";

    private static final String TYPE_FIXED = "fixed";
    private static final String TYPE_PERCENT = "percent";
    private static final String ALIGN_LEFT = "left";
    private static final String ALIGN_MIDDLE = "middle";
    private static final String ALIGN_RIGHT = "right";

    // Futures Tester's colours (dark theme): target green, stop red, entry yellow, zones at 20 %
    private static final Color DEF_TARGET_COLOR = new Color(0x34, 0xc7, 0x59);
    private static final Color DEF_STOP_COLOR = new Color(0xff, 0x45, 0x3a);
    private static final Color DEF_ENTRY_COLOR = new Color(0xfa, 0xcc, 0x15);
    private static final Color DEF_TARGET_FILL = new Color(0x34, 0xc7, 0x59, 51);
    private static final Color DEF_STOP_FILL = new Color(0xff, 0x45, 0x3a, 51);
    private static final Color PLATE = new Color(10, 10, 14, 199);
    private static final Color WARN = new Color(0xf5, 0x9e, 0x0b);
    private static final Color DEF_LABEL_BG = new Color(25, 25, 25);   // dark plate under white text
    private static final Color DEF_ENTRY_LINE = new Color(255, 255, 255, 120);

    private static final int LABEL_PAD_X = 4;
    private static final int LABEL_PAD_Y = 2;
    private static final int DEF_BOX_BARS = 8;   // width of a new box, in bars on screen

    // Binary class name on purpose: the Enums class in mwave_sdk.jar has no InnerClasses
    // attribute, so javac cannot resolve the nested name Enums.ResizeType (same class at runtime).
    private static final com.motivewave.platform.sdk.common.Enums$ResizeType VERTICAL =
            com.motivewave.platform.sdk.common.Enums$ResizeType.VERTICAL;
    private static final com.motivewave.platform.sdk.common.Enums$ResizeType HORIZONTAL =
            com.motivewave.platform.sdk.common.Enums$ResizeType.HORIZONTAL;
    private static final com.motivewave.platform.sdk.common.Enums$ResizeType ALL =
            com.motivewave.platform.sdk.common.Enums$ResizeType.ALL;

    // ==================== State ====================
    private List<Box> boxes = new ArrayList<>();
    private String loadedBoxes;             // last BOXES string applied, to skip no-op reloads
    private ResizePoint activeRP;           // the handle being dragged right now
    private boolean interacting;
    private long lastInteraction;           // ms; guards against a missed end-of-resize callback
    private boolean suppressClick;          // the mouse-up that ends a drag also fires onClick
    private long dragEndedAt;               // when the last drag ended: only a click right after it is that mouse-up
    private java.awt.geom.Point2D lastHover;   // last mouse position over the chart (screen px)
    private volatile long lastHoverAt;         // when the mouse was last over this chart (the R key goes to this one)
    private volatile boolean studySelected;    // the platform painted this study as selected on its last repaint
    private Box lastTouched;                   // the box whose handle was dragged last (Delete falls back to it)
    // whole-box drag bookkeeping
    private double sEntry, sStop, sTarget;
    private long sStart, sEnd;
    private double sStartPx, sEndPx;
    private long loc0T;
    private double loc0V;
    private boolean bodyModeKnown, bodyUsesHover;
    private Boolean armed;                  // TRUE while a box is being placed (the side follows the stop click)
    // placing a box by three clicks: 0 = waiting for the entry, 1 = for the stop, 2 = for the target
    private int placeStep;
    private double placeEntry, placeStop;
    private long placeStart;
    private String placeHint;               // why the last click was not accepted

    private LayerGuard layerGuard;

    /**
     * Starts (once) the guard that keeps this overlay above the user's drawings. The platform hands a click to the LAST layer of the
     * pane that claims the point, and a line the user adds is put last: it lay over the buttons and handles (and the earlier fix
     * only ordered this study's OWN figures, which does not touch the platform's layers).
     */
    private void keepOnTop() {
        if (layerGuard != null) return;
        layerGuard = new LayerGuard(this, this::getDataContext, () -> getSettings().getBoolean(KEEP_ON_TOP, true),
                () -> interacting || armed != null || placeStep != 0 || activeRP != null || panelDragPos != null, t -> java.util.logging.Logger.getLogger("DollarRiskBoxV2").info("LAYERS " + t));
        layerGuard.start();
    }

    private ButtonPanel panel;
    private ClickCatcher catcher;
    private PanelGrip grip;
    private Point panelDragPos;
    private DrawContext lastCtx;            // for turning a click point into price/time

    /** One risk/reward box with its own four drag handles. */
    private final class Box {
        double entry, stop, target;
        long start, end;
        final Handle bodyRP = new Handle(this, ALL);      // whole-box drag from any point of the body
        final Handle stopRP = new Handle(this, VERTICAL);
        final Handle targetRP = new Handle(this, VERTICAL);
        final Handle entryLeftRP = new Handle(this, ALL);
        final Handle entryRightRP = new Handle(this, HORIZONTAL);
        final BoxFigure fig = new BoxFigure(this);

        // bodyRP first: later figures win hit-tests, so the edge handles stay on top of it
        Handle[] handles() { return new Handle[]{bodyRP, stopRP, targetRP, entryLeftRP, entryRightRP}; }
        boolean valid() { return entry > 0 && stop > 0 && target > 0 && start > 0 && end > start; }
    }

    // ==================== Settings UI ====================
    @Override
    public void initialize(Defaults defaults) {
        var sd = createSD();

        var general = sd.addTab(get("TAB_GENERAL"));
        var risk = general.addGroup(get("GRP_RISK"));
        var typeOptions = new ArrayList<NVP>();
        typeOptions.add(new NVP(get("LBL_TYPE_FIXED"), TYPE_FIXED));
        typeOptions.add(new NVP(get("LBL_TYPE_PERCENT"), TYPE_PERCENT));
        risk.addRow(new DiscreteDescriptor(RISK_TYPE, get("LBL_RISK_TYPE"), TYPE_FIXED, typeOptions));
        risk.addRow(new DoubleDescriptor(RISK_AMOUNT, get("LBL_RISK_AMOUNT"), 200.0, 0.01, 10000000, 1));
        risk.addRow(new DoubleDescriptor(RISK_PERCENT, get("LBL_RISK_PERCENT"), 0.4, 0.01, 100, 0.05));
        risk.addRow(new DoubleDescriptor(BALANCE, get("LBL_BALANCE"), 50000.0, 0, 1000000000, 100));
        risk.addRow(new IntegerDescriptor(MAX_QTY, get("LBL_MAX_QTY"), 40, 1, 100000, 1));
        var costs = general.addGroup(get("GRP_COSTS"));
        var commOptions = new ArrayList<NVP>();
        commOptions.add(new NVP(get("OPT_COMM_AUTO"), Costs.Mode.AUTO.name()));
        commOptions.add(new NVP(get("OPT_COMM_LUCID"), Costs.Mode.LUCID.name()));
        commOptions.add(new NVP(get("OPT_COMM_MANUAL"), Costs.Mode.MANUAL.name()));
        costs.addRow(new DiscreteDescriptor(COMM_MODE, get("LBL_COMM_MODE"), Costs.Mode.AUTO.name(), commOptions));
        costs.addRow(new DoubleDescriptor(COMMISSION, get("LBL_COMMISSION"), 0.50, 0, 1000, 0.01));
        costs.addRow(new IntegerDescriptor(SLIPPAGE, get("LBL_SLIPPAGE"), 1, 0, 100, 1));
        var pnl = general.addGroup(get("LBL_PANEL"));
        pnl.addRow(new BooleanDescriptor(SHOW_PANEL, get("LBL_SHOW_PANEL"), true));
        pnl.addRow(new BooleanDescriptor(HOTKEY_R, get("LBL_HOTKEY_R"), true));
        pnl.addRow(new IntegerDescriptor(BOX_WIDTH, get("LBL_BOX_WIDTH"), DEF_BOX_BARS, 2, 500, 1));
        pnl.addRow(new BooleanDescriptor(ESC_CANCELS, get("LBL_ESC_CANCELS"), true));
        pnl.addRow(new BooleanDescriptor(DEL_REMOVES, get("LBL_DEL_REMOVES"), true));
        pnl.addRow(new BooleanDescriptor(ESC_DELETES, get("LBL_ESC_DELETES"), true));
        pnl.addRow(new BooleanDescriptor(KEEP_ON_TOP, get("LBL_KEEP_ON_TOP"), true));

        var format = sd.addTab(get("TAB_FORMAT"));
        var f = format.addGroup("");
        f.addRow(new ColorDescriptor(TARGET_COLOR, get("LBL_TARGET_COLOR"), DEF_TARGET_COLOR),
                new ColorDescriptor(TARGET_FILL, get("LBL_FILL"), DEF_TARGET_FILL));
        f.addRow(new ColorDescriptor(STOP_COLOR, get("LBL_STOP_COLOR"), DEF_STOP_COLOR),
                new ColorDescriptor(STOP_FILL, get("LBL_FILL"), DEF_STOP_FILL));
        f.addRow(new ColorDescriptor(ENTRY_COLOR, get("LBL_ENTRY_COLOR"), DEF_ENTRY_COLOR));
        f.addRow(new FontDescriptor(FONT, get("LBL_FONT"), new Font("SansSerif", Font.BOLD, 12)));
        f.addRow(new BooleanDescriptor(SHOW_LABELS, get("LBL_SHOW_LABELS"), true),
                new BooleanDescriptor(SHOW_PRICES, get("LBL_SHOW_PRICES"), false));

        createRD();
        sd.addQuickSettings(RISK_TYPE, RISK_AMOUNT, RISK_PERCENT, BALANCE, COMM_MODE, COMMISSION);
    }

    // ==================== Lifecycle ====================
    /** Study.clone() is shallow for subclass fields. Never share boxes or figures with a
     * settings preview / chart copy: each figure's inner-class owner must be the copy. */
    @Override
    public DollarRiskBoxV2 clone() {
        DollarRiskBoxV2 copy = (DollarRiskBoxV2) super.clone();
        if (copy == null) return null;
        copy.boxes = new ArrayList<>();
        for (Box b : boxes) {
            Box next = copy.new Box();
            next.entry = b.entry; next.stop = b.stop; next.target = b.target;
            next.start = b.start; next.end = b.end;
            copy.boxes.add(next);
        }
        copy.loadedBoxes = loadedBoxes;
        copy.activeRP = null;
        copy.interacting = false;
        copy.lastInteraction = 0;
        copy.suppressClick = false;
        copy.dragEndedAt = 0;
        copy.lastHover = null;
        copy.lastHoverAt = 0;
        copy.studySelected = false;
        copy.lastTouched = null;
        copy.armed = null;
        copy.placeStep = 0;
        copy.placeHint = null;
        copy.layerGuard = null;
        copy.panel = null; copy.catcher = null; copy.grip = null;
        copy.panelDragPos = null; copy.lastCtx = null;
        copy.clearFigures();
        return copy;
    }

    @Override
    public void destroy() {
        if (layerGuard != null) {
            layerGuard.stop();
            layerGuard = null;
        }
        PLACING.remove(this);
        INSTANCES.remove(this);
        super.destroy();
    }
    @Override
    public void clearState() {
        // Called by the platform as routine housekeeping (also after every settings write):
        // never drop the boxes here - they live in the settings. Figures are re-added below.
        super.clearState();
        clearFigures();
    }

    @Override
    protected void calculateValues(DataContext ctx) {
        if (interacting && System.currentTimeMillis() - lastInteraction > 2000) {
            interacting = false;
            activeRP = null;
        }
        if (!interacting) loadFromSettings();
        keepOnTop();
        syncFigures();
        notifyRedraw();
    }

    @Override
    public void onBarUpdate(DataContext ctx) {
        syncFigures();   // the platform may clear figures; put everything back
        notifyRedraw();  // live P/L on the entry labels
    }

    @Override
    public void onSettingsUpdated(DataContext ctx) {
        super.onSettingsUpdated(ctx);
        if (!interacting) loadFromSettings();
        syncFigures();
        notifyRedraw();
    }

    // ==================== Persistence ====================
    private void loadFromSettings() {
        String str = getSettings().getString(BOXES, "");
        if (str == null) str = "";
        if (str.equals(loadedBoxes)) return;
        loadedBoxes = str;
        clearFigures();
        boxes.clear();
        for (String part : str.split(";")) {
            String[] v = part.split(",");
            if (v.length != 5) continue;
            try {
                Box b = new Box();
                b.entry = Double.parseDouble(v[0]);
                b.stop = Double.parseDouble(v[1]);
                b.target = Double.parseDouble(v[2]);
                b.start = Long.parseLong(v[3]);
                b.end = Long.parseLong(v[4]);
                if (b.valid()) boxes.add(b);
            } catch (NumberFormatException ignored) { }
        }
    }

    private void saveBoxes() {
        StringBuilder sb = new StringBuilder();
        for (Box b : boxes) {
            if (sb.length() > 0) sb.append(';');
            sb.append(b.entry).append(',').append(b.stop).append(',').append(b.target)
              .append(',').append(b.start).append(',').append(b.end);
        }
        loadedBoxes = sb.toString();     // set first: the write below triggers a reload
        getSettings().setString(BOXES, loadedBoxes);
    }

    /** Make sure every figure we own is on the chart (the platform clears them freely). */
    private void syncFigures() {
        registerKeys();
        if (panel == null) {
            catcher = new ClickCatcher();
            panel = new ButtonPanel();
            grip = new PanelGrip();
        }
        List<?> figs = getFigures();
        if (!figs.contains(catcher)) addFigure(catcher);
        for (Box b : boxes) {
            if (!figs.contains(b.fig)) addFigure(b.fig);
            for (Handle h : b.handles()) if (!figs.contains(h)) addFigure(h);
        }
        if (!figs.contains(panel)) addFigure(panel);
        if (!figs.contains(grip)) addFigure(grip);
        // The panel and its grip must stay the LAST figures: anything added after them (a box placed later) is drawn over them
        // and wins the mouse, which is how new lines ended up lying on top of the buttons again. Not while the grip is dragged.
        List<?> now = getFigures();
        int n = now.size();
        if (n >= 2 && (now.get(n - 2) != panel || now.get(n - 1) != grip) && panelDragPos == null && activeRP != grip) {
            removeFigure(panel);
            removeFigure(grip);
            addFigure(panel);
            addFigure(grip);
        }
    }

    private void removeBox(Box b) {
        boxes.remove(b);
        removeFigure(b.fig);
        for (Handle h : b.handles()) removeFigure(h);
        saveBoxes();
        notifyRedraw();
    }

    // ==================== Placing a box ====================
    /**
     * End time of a new box that starts at {@code start}: the width on SCREEN of that many bars.
     * Measured in pixels, not as "start + N bar durations": the time axis is not linear (session
     * gaps, the compressed empty area right of the last bar), so a time span can come out several
     * times wider than intended.
     */
    private long boxEnd(long start) {
        int bars = Math.max(1, getSettings().getInteger(BOX_WIDTH, DEF_BOX_BARS));
        DrawContext ctx = lastCtx;
        DataContext dc = getDataContext();
        var series = dc == null ? null : dc.getDataSeries();
        if (series == null || series.size() < 2) return start + 60_000L * bars;
        int last = series.size() - 1;
        if (ctx != null) {
            double perBar = ctx.translateTimeD(series.getStartTime(last)) - ctx.translateTimeD(series.getStartTime(last - 1));
            if (perBar >= 1) {
                long end = ctx.translate2Time(ctx.translateTimeD(start) + perBar * bars);
                if (end > start) return end;
            }
        }
        return start + Math.max(1, series.getStartTime(last) - series.getStartTime(last - 1)) * bars;
    }

    private void addBox(double entry, double stop, double target, long start) {
        DataContext dc = getDataContext();
        if (dc == null) return;
        Instrument instr = dc.getInstrument();
        Box b = new Box();
        b.entry = instr.round(entry);
        b.stop = instr.round(stop);
        b.target = instr.round(target);
        b.start = start;
        b.end = boxEnd(start);
        if (!b.valid()) return;
        boxes.add(b);
        saveBoxes();
        syncFigures();
        notifyRedraw();
    }

    private void stopPlacing() {
        PLACING.remove(this);
        armed = null;
        placeStep = 0;
        placeHint = null;
    }

    /**
     * A chart click while the tool is on: the entry, then the stop, then the target (Futures Tester D-090). The side
     * follows the stop - below the entry a buy, above a sell; the target must lie on the other side; both at least a
     * tick from the entry, otherwise the click is refused with a hint and the box is not made.
     */
    private void placeClick(Point p) {
        DrawContext ctx = lastCtx;
        DataContext dc = getDataContext();
        if (ctx == null || dc == null) {
            stopPlacing();
            return;
        }
        Instrument instr = dc.getInstrument();
        double tick = instr.getTickSize(), eps = tick / 2;
        double price = instr.round(ctx.translate2Value(p.y));
        placeHint = null;
        if (placeStep == 0) {
            placeEntry = price;
            placeStart = ctx.translate2Time(p.x);
            placeStep = 1;
        } else if (placeStep == 1) {
            if (Math.abs(price - placeEntry) < tick - eps) {
                placeHint = get("HINT_STOP_OFF_ENTRY");
            } else {
                placeStop = price;
                placeStep = 2;
            }
        } else {
            boolean buy = placeStop < placeEntry;
            if (buy ? price < placeEntry + tick - eps : price > placeEntry - tick + eps) {
                placeHint = get("HINT_TARGET_OTHER_SIDE");
            } else {
                double entry = placeEntry, stop = placeStop;
                long start = placeStart;
                stopPlacing();
                addBox(entry, stop, price, start);
                return;
            }
        }
        notifyRedraw();
    }

    /** The tool on or off (the panel button, or R over the chart): a second press cancels the box being placed. */
    private void toggleTool() {
        boolean off = armed != null;
        stopPlacing();
        if (!off) { armed = Boolean.TRUE; trackPlacing(); }
        notifyRedraw();
    }

    // ==================== Escape and Delete ====================
    // The SDK gives a study no keyboard events, but the platform is a JavaFX application: a key filter
    // on its windows sees the keys.
    //  - Escape cancels placement, otherwise removes a selected box. Completed-box deletion
    //    is consumed so the platform cannot deselect or remove the entire indicator first.
    //  - Delete / Backspace remove the selected box and are consumed, so the platform does not delete the
    //    whole indicator. They act only while this study is painted as selected AND a box is under the mouse
    //    (or was dragged last); otherwise the key goes to the platform untouched, and so does every key typed
    //    into a text field.
    private static final java.util.Set<DollarRiskBoxV2> PLACING =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));
    private static final java.util.Set<DollarRiskBoxV2> INSTANCES =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    // Register live instances in callbacks too: Java cloning does not run initializers.
    private void registerKeys() {
        INSTANCES.add(this);
        KeyFilter.installOccasionally();
    }

    private void trackPlacing() {
        PLACING.add(this);
        KeyFilter.install();
    }

    /** Escape (platform UI thread): drop the box being placed - nothing is left on the chart. */
    private void escapePressed() {
        if (armed == null || !getSettings().getBoolean(ESC_CANCELS, true)) return;
        stopPlacing();
        notifyRedraw();
    }

    /** The box a Delete press would remove, or null when this study should leave the key alone. */
    private Box deleteTarget() {
        return target(DEL_REMOVES);
    }

    /** The box an Escape press would remove (nothing is being placed): the selected indicator's box under the mouse. */
    private Box escapeTarget() {
        Box b = target(ESC_DELETES);
        // the indicator is selected and has exactly one box: that is the one the trader means, wherever the mouse is
        if (b == null && studySelected && boxes.size() == 1 && getSettings().getBoolean(ESC_DELETES, true)) return boxes.get(0);
        return b;
    }

    private Box target(String setting) {
        if (!studySelected || boxes.isEmpty() || !getSettings().getBoolean(setting, true)) return null;
        java.awt.geom.Point2D h = lastHover;
        if (h != null) {
            for (int i = boxes.size() - 1; i >= 0; i--) {
                if (boxes.get(i).fig.bodyContains(h.getX(), h.getY())) return boxes.get(i);
            }
        }
        if (lastTouched != null && boxes.contains(lastTouched)) return lastTouched;
        return boxes.size() == 1 ? boxes.get(0) : null;
    }

    /**
     * The key filter: a JavaFX filter on the platform's windows (the platform's UI is pure JavaFX, nothing
     * else sees the keys). Kept in its own class so that a platform without JavaFX loses only these two
     * features, not the study.
     */
    private static final class KeyFilter {
        private static final String PROPERTY = "DollarRiskBoxV2.escapeFilter";
        private static volatile long lastInstall;
        private static boolean failureLogged;
        private static final javafx.event.EventHandler<javafx.scene.input.KeyEvent> FILTER = e -> {
            if (e.isConsumed()) return;      // JavaFX still calls every filter: V1's may have taken this key already
            javafx.scene.input.KeyCode code = e.getCode();
            boolean r = isR(e);
            if (code != javafx.scene.input.KeyCode.ESCAPE && code != javafx.scene.input.KeyCode.DELETE
                    && code != javafx.scene.input.KeyCode.BACK_SPACE && !r) return;
            if (e.isShortcutDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown() || e.isShiftDown() && r) return;
            if (typing(e.getTarget())) return;
            if (e.getSource() instanceof javafx.stage.Window window && window.getScene() != null
                    && typing(window.getScene().getFocusOwner())) return;
            if (r) {
                // only the chart under the mouse (hovered in the last second and a half), so R in another window or
                // over another chart does nothing here
                DollarRiskBoxV2 best = null;
                synchronized (INSTANCES) {
                    for (DollarRiskBoxV2 st : INSTANCES)
                        if (st.getSettings().getBoolean(HOTKEY_R, true) && System.currentTimeMillis() - st.lastHoverAt < 1500
                                && (best == null || st.lastHoverAt > best.lastHoverAt)) best = st;
                }
                if (best == null) return;
                e.consume();
                best.toggleTool();
                return;
            }
            if (code == javafx.scene.input.KeyCode.ESCAPE) {
                DollarRiskBoxV2[] placing;
                synchronized (PLACING) { placing = PLACING.toArray(new DollarRiskBoxV2[0]); }
                boolean cancelled = false;
                for (DollarRiskBoxV2 st : placing) {
                    if (st.armed == null) { PLACING.remove(st); continue; }
                    if (!st.getSettings().getBoolean(ESC_CANCELS, true)) continue;
                    st.escapePressed();
                    cancelled = true;
                }
                // Stale / disabled placement entries must not disable deletion of completed boxes.
                if (cancelled) return;
            }
            DollarRiskBoxV2[] studies;
            synchronized (INSTANCES) { studies = INSTANCES.toArray(new DollarRiskBoxV2[0]); }
            for (DollarRiskBoxV2 st : studies) {
                DollarRiskBoxV2.Box b = code == javafx.scene.input.KeyCode.ESCAPE ? st.escapeTarget() : st.deleteTarget();
                if (b == null) continue;
                e.consume();
                // Filters already run on the JavaFX UI thread; do not let deselection or a
                // settings reload replace the target before the queued removal runs.
                st.removeBox(b);
                System.out.println("DollarRiskBoxV2: " + code + " removed box; remaining=" + st.boxes.size());
                return;
            }
        };

        /** R by the key or by the letter it types: with a Russian layout the key code may come as UNDEFINED. */
        private static boolean isR(javafx.scene.input.KeyEvent e) {
            if (e.getCode() == javafx.scene.input.KeyCode.R) return true;
            String t = e.getText();
            return t != null && (t.equalsIgnoreCase("r") || t.equalsIgnoreCase("\u043a"));
        }

        private static boolean typing(javafx.event.EventTarget target) {
            if (!(target instanceof javafx.scene.Node node)) return false;
            for (javafx.scene.Node n = node; n != null; n = n.getParent()) {
                if (n instanceof javafx.scene.control.TextInputControl) return true;
            }
            return false;
        }

        /** Idempotent; also replaces the filter of an earlier version of this study that was reloaded
         *  without restarting the platform. Put on the window itself: the first stop of every key event. */
        @SuppressWarnings("unchecked")
        static synchronized void install() {
            if (!javafx.application.Platform.isFxApplicationThread()) {
                javafx.application.Platform.runLater(KeyFilter::install);
                return;
            }
            lastInstall = System.currentTimeMillis();
            try {
                for (javafx.stage.Window w : new ArrayList<>(javafx.stage.Window.getWindows())) {
                    Object old = w.getProperties().get(PROPERTY);
                    if (old == FILTER) continue;
                    if (old instanceof javafx.event.EventHandler<?> h) {
                        w.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                                (javafx.event.EventHandler<javafx.scene.input.KeyEvent>) h);
                    }
                    w.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, FILTER);
                    w.getProperties().put(PROPERTY, FILTER);
                }
            } catch (Throwable failure) {
                if (!failureLogged) {
                    failureLogged = true;
                    System.err.println("DollarRiskBoxV2: keyboard filter unavailable: " + failure);
                }
            }
        }

        /** Mouse moves are frequent and windows rarely appear: look again at most every few seconds. */
        static void installOccasionally() {
            if (System.currentTimeMillis() - lastInstall > 5000) install();
        }
    }

    // ==================== Interaction ====================
    @Override
    public boolean onClick(Point p, int flags) {
        registerKeys();
        lastHover = new java.awt.geom.Point2D.Double(p.x, p.y);
        if (suppressClick) {           // mouse-up of a drag, not a real click
            suppressClick = false;
            // 9 Nov 2026: when the platform ends a drag WITHOUT a click, the flag stayed and ate the next real press
            // (a button "needed a second click"); only a click within 400 ms of the drag's end is its mouse-up
            if (System.currentTimeMillis() - dragEndedAt < 400) return true;
        }
        for (Box b : new ArrayList<>(boxes)) {
            if (b.fig.deleteRect != null && b.fig.deleteRect.contains(p)) {
                removeBox(b);
                return true;
            }
        }
        if (panel != null && panel.gripRect != null && panel.gripRect.contains(p)) return true;  // grip: drag only
        if (panel != null && panel.toolRect != null && panel.toolRect.contains(p)) {
            toggleTool();                                  // pressing the lit button again cancels
            return true;
        }
        if (armed != null) {
            placeClick(p);
            return true;
        }
        for (int i = boxes.size() - 1; i >= 0; i--) {
            Box b = boxes.get(i);
            if (b.fig.bodyContains(p.x, p.y)) {
                lastTouched = b;
                return false; // the platform still selects the indicator
            }
        }
        return false;
    }

    /** Puts the panel's top-left at the clicked point (stored as a fraction of the chart size). */
    private void movePanelTo(Point p) {
        if (lastCtx == null) return;
        Rectangle bd = lastCtx.getBounds();
        double fx = (p.x - bd.x) / (double) Math.max(1, bd.width);
        double fy = (p.y - bd.y) / (double) Math.max(1, bd.height);
        getSettings().setDouble(PANEL_FX, Math.max(0, Math.min(1, fx)));
        getSettings().setDouble(PANEL_FY, Math.max(0, Math.min(1, fy)));
        notifyRedraw();
    }

    @Override
    public void onHover(java.awt.geom.Point2D loc, int flags, DrawContext ctx) {
        lastHover = loc;
        lastHoverAt = System.currentTimeMillis();
        lastCtx = ctx;
        registerKeys();
        if (armed != null && placeStep > 0) notifyRedraw();    // the preview follows the mouse
    }

    @Override
    public void onBeginResize(ResizePoint rp, DrawContext ctx) {
        registerKeys();
        studySelected = ctx.isSelected();
        if (rp instanceof Handle th) lastTouched = th.owner;
        if (rp instanceof Handle h && h == h.owner.bodyRP) {
            Box b = h.owner;
            sEntry = b.entry; sStop = b.stop; sTarget = b.target; sStart = b.start; sEnd = b.end;
            sStartPx = ctx.translateTimeD(sStart);
            sEndPx = ctx.translateTimeD(sEnd);
            loc0T = rp.getTime();
            loc0V = rp.getValue();
            bodyModeKnown = false;
        }
        activeRP = rp;
        interacting = true;
        lastInteraction = System.currentTimeMillis();
    }

    @Override
    public void onResize(ResizePoint rp, DrawContext ctx) {
        if (rp == null) return;
        activeRP = rp;
        interacting = true;
        lastInteraction = System.currentTimeMillis();

        if (rp == grip) {
            // Absolute handle (time/price) -> back to screen pixels for the screen-fixed panel.
            java.awt.geom.Point2D pt = ctx.translate(rp.getTime(), rp.getValue());
            panelDragPos = new Point((int) pt.getX() - ButtonPanel.GW / 2, (int) pt.getY() - ButtonPanel.BH / 2);
            notifyRedraw();
            return;
        }
        if (!(rp instanceof Handle h)) return;
        Box b = h.owner;
        Instrument instr = ctx.getDataContext().getInstrument();
        double v = instr.round(rp.getValue());
        long t = rp.getTime();
        if (h == b.bodyRP) {
            // The handle may be reported either at the mouse position or shifted with the mouse;
            // decide once, on the first event, by which reference it is closer to.
            java.awt.geom.Point2D now = ctx.translate(t, rp.getValue());
            if (!bodyModeKnown) {
                double dHover = lastHover == null ? Double.MAX_VALUE : now.distance(lastHover);
                double dLoc = now.distance(ctx.translate(loc0T, loc0V));
                bodyUsesHover = dHover < dLoc;
                bodyModeKnown = true;
            }
            // Horizontal in PIXELS: the time axis is not linear (session gaps), so a time delta
            // measured at the mouse is wrong for the box edges elsewhere on the axis.
            double baseX = bodyUsesHover ? lastHover.getX() : ctx.translate(loc0T, loc0V).getX();
            double baseV = bodyUsesHover ? ctx.translate2Value(lastHover.getY()) : loc0V;
            double dx = now.getX() - baseX;
            double dv = rp.getValue() - baseV;
            b.entry = instr.round(sEntry + dv);
            b.stop = instr.round(sStop + dv);
            b.target = instr.round(sTarget + dv);
            b.start = ctx.translate2Time(sStartPx + dx);
            b.end = ctx.translate2Time(sEndPx + dx);
            notifyRedraw();
            return;
        }
        double tick = instr.getTickSize();
        if (h == b.stopRP) {
            // as in the tester: an edge cannot cross the entry - the box keeps its side
            b.stop = b.stop < b.entry ? Math.min(v, instr.round(b.entry - tick)) : Math.max(v, instr.round(b.entry + tick));
        } else if (h == b.targetRP) {
            b.target = b.target > b.entry ? Math.max(v, instr.round(b.entry + tick)) : Math.min(v, instr.round(b.entry - tick));
        } else if (h == b.entryLeftRP) {
            // The entry handle moves the whole box (price and time), keeping stop/target distances.
            double dv = v - b.entry;
            b.stop = instr.round(b.stop + dv);
            b.target = instr.round(b.target + dv);
            b.entry = v;
            if (t > 0) {
                long dt = t - b.start;
                b.start += dt;
                b.end += dt;
            }
        } else if (h == b.entryRightRP) {
            if (t > b.start) b.end = t;
        }
        // NOT saved here: writing settings makes the platform call clearState(), which removes
        // the handles mid-drag and aborts the drag. Saved once in onEndResize().
        notifyRedraw();
    }

    @Override
    public void onEndResize(ResizePoint rp, DrawContext ctx) {
        if (rp == grip) {
            if (panelDragPos != null) movePanelTo(new Point(panelDragPos.x, panelDragPos.y));
            panelDragPos = null;
        } else {
            saveBoxes();
        }
        interacting = false;
        activeRP = null;
        suppressClick = true;
        dragEndedAt = System.currentTimeMillis();
        syncFigures();
        notifyRedraw();
    }

    @Override
    public MenuDescriptor onMenu(String plotName, Point loc, DrawContext ctx) {
        var items = new ArrayList<MenuItem>();
        Box hit = null;
        for (int i = boxes.size() - 1; i >= 0; i--) {
            if (boxes.get(i).fig.bodyContains(loc.x, loc.y)) { hit = boxes.get(i); break; }
        }
        if (hit != null) {
            final Box b = hit;
            items.add(new MenuItem(get("LBL_DELETE_BOX"), () -> removeBox(b)));
            items.add(new MenuItem(get("LBL_FLIP"), () -> {
                b.stop = b.entry + (b.entry - b.stop);
                b.target = b.entry - (b.target - b.entry);
                saveBoxes();
                notifyRedraw();
            }));
        }
        if (!boxes.isEmpty()) {
            items.add(new MenuItem(get("LBL_DELETE_ALL"), () -> {
                for (Box b : new ArrayList<>(boxes)) {
                    removeFigure(b.fig);
                    for (Handle h : b.handles()) removeFigure(h);
                }
                boxes.clear();
                saveBoxes();
                notifyRedraw();
            }));
        }
        items.add(new MenuItem(get("LBL_RESET_PANEL"), () -> {
            getSettings().setDouble(PANEL_FX, -1.0);
            getSettings().setDouble(PANEL_FY, -1.0);
            notifyRedraw();
        }));
        // On a box: only our items - the platform's "Delete" would remove the whole study
        // (panel and every box). Elsewhere keep the platform's items (Properties etc.).
        return new MenuDescriptor(items, hit == null);
    }

    // ==================== Math ====================
    /** The commission this chart's boxes count, and where it comes from. */
    private Costs.Commission commission(Instrument instr) {
        Settings s = getSettings();
        Costs.Mode mode = Costs.Mode.parse(s.getString(COMM_MODE, Costs.Mode.AUTO.name()));
        return Costs.of(mode, mode == Costs.Mode.AUTO ? Costs.platformServer() : null, rootSymbol(instr), s.getDouble(COMMISSION, 0.50));
    }

    /** "MNQZ6" -> "MNQ"; the platform's underlying only when the symbol does not look like a contract. */
    private static String rootSymbol(Instrument instr) {
        String sym = instr.getSymbol();
        String r = Costs.root(sym);
        if (sym != null && r.equalsIgnoreCase(sym)) {
            try {
                String u = instr.getUnderlying();
                if (u != null && !u.isBlank()) return Costs.root(u);
            } catch (RuntimeException ignored) { }
        }
        return r;
    }

    /** The box sized exactly as Futures Tester sizes it (core.RiskSize). */
    private RiskSize.Result size(DataContext ctx, double entry, double stop, double target) {
        Instrument instr = ctx.getInstrument();
        Settings s = getSettings();
        double ps = instr.getPointSize() > 0 ? instr.getPointSize() : 1;
        double tick = instr.getTickSize() > 0 ? instr.getTickSize() : 0.01;
        double tickValue = tick / ps * instr.getPointValue();
        boolean percent = TYPE_PERCENT.equals(s.getString(RISK_TYPE, TYPE_FIXED));
        var spec = new RiskSize.Spec(tick, tickValue > 0 ? tickValue : 0.01, Math.max(0, s.getInteger(SLIPPAGE, 1)),
                commission(instr).perSide(), s.getDouble(BALANCE, 50000), Math.max(1, s.getInteger(MAX_QTY, 40)));
        return RiskSize.of(entry, stop, target, spec,
                new RiskSize.Budget(percent, percent ? s.getDouble(RISK_PERCENT, 0.4) : s.getDouble(RISK_AMOUNT, 200)), 0);
    }

    /** True when the point lies on this study's own tool panel (grip or a button) plus a small margin. Boxes and their handles must
     *  never claim those points, otherwise a box line or fill passing behind the panel makes it impossible to press or drag. */
    private boolean onPanel(double x, double y) {
        ButtonPanel p = panel;
        if (p == null) return false;
        return near(p.gripRect, x, y) || near(p.toolRect, x, y);
    }

    private static boolean near(Rectangle r, double x, double y) {
        return r != null && x >= r.x - 3 && x <= r.x + r.width + 3 && y >= r.y - 3 && y <= r.y + r.height + 3;
    }

    // ==================== Figures ====================
    /** Platform drag handle of one box. */
    private class Handle extends ResizePoint {
        final Box owner;

        Handle(Box owner, com.motivewave.platform.sdk.common.Enums$ResizeType type) {
            super(type, true);
            this.owner = owner;
            setFillColor(DEF_TARGET_COLOR);
            setOutlineColor(Color.WHITE);
        }

        @Override
        public void layout(DrawContext ctx) {
            // Every handle follows its box live; only the one under the mouse is left to the platform.
            if (this != activeRP) {
                if (this == owner.bodyRP) setLocation(owner.start + (owner.end - owner.start) / 2, owner.entry);
                else if (this == owner.stopRP) setLocation(owner.start, owner.stop);
                else if (this == owner.targetRP) setLocation(owner.start, owner.target);
                else if (this == owner.entryLeftRP) setLocation(owner.start, owner.entry);
                else if (this == owner.entryRightRP) setLocation(owner.end, owner.entry);
            }
            super.layout(ctx);
        }

        /** The platform still draws/hit-tests study handles only while the study is selected;
         *  this keeps them alive for every valid box. */
        @Override
        public boolean isVisible(DrawContext ctx) {
            return owner.valid();
        }

        /** The body handle is the whole box: hit anywhere on the fill/labels (not on the delete
         *  button); while it is being dragged it must claim every point or the platform drops it. */
        @Override
        public boolean contains(double x, double y, DrawContext ctx) {
            if (activeRP != this && onPanel(x, y)) return false;
            if (this != owner.bodyRP) return super.contains(x, y, ctx);
            if (activeRP == this) return true;
            BoxFigure f = owner.fig;
            if (!f.bodyContains(x, y) || (f.deleteRect != null && f.deleteRect.contains(x, y))) return false;
            // leave the neighbourhood of the four edge handles to those handles
            for (Handle h : owner.handles()) {
                if (h == this) continue;
                java.awt.geom.Point2D hp = ctx.translate(h.getTime(), h.getValue());
                if (Math.abs(x - hp.getX()) <= 10 && Math.abs(y - hp.getY()) <= 10) return false;
            }
            return true;
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx) {
            if (this == owner.bodyRP) return;     // invisible
            java.awt.geom.Point2D pt = ctx.translate(getTime(), getValue());
            int r = 4;
            int x = (int) Math.round(pt.getX()), y = (int) Math.round(pt.getY());
            gc.setColor(DEF_TARGET_COLOR);
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setColor(Color.WHITE);
            gc.setStroke(new BasicStroke(1f));
            gc.drawOval(x - r, y - r, r * 2, r * 2);
        }
    }

    /** Invisible, covers the chart only while Long/Short is armed, so the next click comes to us. */
    private class ClickCatcher extends Figure {
        @Override
        public void layout(DrawContext ctx) {
            lastCtx = ctx;
            setBounds(ctx.getBounds());
        }

        /** While a box is being placed: the lines chosen so far (entry yellow, the next level dashed under the mouse),
         *  and what the next click would give - contracts and risk at the stop, the target and R:R at the target. */
        @Override
        public void draw(Graphics2D gc, DrawContext ctx) {
            java.awt.geom.Point2D hover = lastHover;
            if (armed == null || placeStep == 0 || hover == null) return;
            DataContext dc = ctx.getDataContext();
            Instrument instr = dc.getInstrument();
            Settings s = getSettings();
            int xa = ctx.translateTime(placeStart), xb = Math.max(xa + 40, ctx.translateTime(boxEnd(placeStart)));
            int yE = ctx.translateValue(placeEntry);
            double price = instr.round(ctx.translate2Value(hover.getY()));
            int yH = ctx.translateValue(price);
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // the zones already while placing (Alexei, 9 Nov 2026: "not visible while placing"): the stop zone follows
            // the mouse after the entry click, the target zone after the stop click - in the box's own fill colours
            double tickP = instr.getTickSize();
            if (placeStep == 1) {
                if (Math.abs(price - placeEntry) >= tickP / 2) {
                    gc.setColor(s.getColor(STOP_FILL, DEF_STOP_FILL));
                    gc.fillRect(xa, Math.min(yE, yH), xb - xa, Math.abs(yH - yE));
                }
            } else {
                int yS0 = ctx.translateValue(placeStop);
                gc.setColor(s.getColor(STOP_FILL, DEF_STOP_FILL));
                gc.fillRect(xa, Math.min(yE, yS0), xb - xa, Math.abs(yS0 - yE));
                boolean buy0 = placeStop < placeEntry;
                if (buy0 ? price > placeEntry : price < placeEntry) {
                    gc.setColor(s.getColor(TARGET_FILL, DEF_TARGET_FILL));
                    gc.fillRect(xa, Math.min(yE, yH), xb - xa, Math.abs(yH - yE));
                }
            }
            Stroke dashed = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f);
            gc.setStroke(new BasicStroke(2f));
            gc.setColor(s.getColor(ENTRY_COLOR, DEF_ENTRY_COLOR));
            gc.drawLine(xa, yE, xb, yE);
            String text;
            Color color;
            if (placeStep == 1) {
                gc.setColor(s.getColor(STOP_COLOR, DEF_STOP_COLOR));
                gc.setStroke(dashed);
                gc.drawLine(xa, yH, xb, yH);
                boolean buy = price < placeEntry;
                RiskSize.Result r = size(dc, placeEntry, price, buy ? placeEntry + 1e6 : placeEntry - 1e6);
                text = r.valid() ? (buy ? "Buy " : "Sell ") + r.qty() + " " + rootSymbol(instr) + " \u00b7 risk " + Labels.usd(r.risk())
                        + " (" + r.stopTicks() + " ticks)" : get("HINT_STOP_OFF_ENTRY");
                color = s.getColor(STOP_COLOR, DEF_STOP_COLOR);
            } else {
                int yS = ctx.translateValue(placeStop);
                gc.setColor(s.getColor(STOP_COLOR, DEF_STOP_COLOR));
                gc.setStroke(new BasicStroke(2f));
                gc.drawLine(xa, yS, xb, yS);
                gc.setColor(s.getColor(TARGET_COLOR, DEF_TARGET_COLOR));
                gc.setStroke(dashed);
                gc.drawLine(xa, yH, xb, yH);
                RiskSize.Result r = size(dc, placeEntry, placeStop, price);
                text = r.valid() ? "Target +" + Labels.usd(r.reward()) + " \u00b7 R:R " + String.format(Locale.US, "%.2f", r.rr())
                        + " \u00b7 " + (r.buy() ? "Buy " : "Sell ") + r.qty() : get("HINT_TARGET_OTHER_SIDE");
                color = r.valid() ? s.getColor(TARGET_COLOR, DEF_TARGET_COLOR) : WARN;
            }
            plate(gc, labelFont(), text, xa + 6, yH + (yH < yE ? -14 : 14), color, ctx.getBounds());
        }

        @Override
        public boolean contains(double x, double y, DrawContext ctx) {
            return armed != null;
        }

        @Override
        public boolean isVisible(DrawContext ctx) {
            return true;
        }
    }

    /** Tool panel: [grip] [Risk box]. Display only - nothing here sends orders; yellow, so it is not Risk Arm's green panel. */
    private class ButtonPanel extends Figure {
        static final int GW = 14, BW = 104, BH = 40, MARGIN = 10, GAP = 4;
        Rectangle gripRect, toolRect;
        int px, py;
        private boolean visible;
        private Font font, small;

        @Override
        public void layout(DrawContext ctx) {
            lastCtx = ctx;
            Settings s = getSettings();
            visible = s.getBoolean(SHOW_PANEL, true);
            if (!visible) {
                gripRect = toolRect = null;
                return;
            }
            Rectangle b = ctx.getBounds();
            int w = GW + GAP + BW;
            double fx = s.getDouble(PANEL_FX, -1);
            double fy = s.getDouble(PANEL_FY, -1);
            if (panelDragPos != null) {        // live while the grip is dragged
                px = panelDragPos.x;
                py = panelDragPos.y;
            } else if (fx < 0 || fy < 0) {     // default: top centre (Risk Arm and V1 default to the top right)
                px = b.x + (b.width - w) / 2;
                py = b.y + MARGIN;
            } else {
                px = b.x + (int) Math.round(fx * b.width);
                py = b.y + (int) Math.round(fy * b.height);
            }
            px = Math.max(b.x, Math.min(px, b.x + b.width - w));
            py = Math.max(b.y, Math.min(py, b.y + b.height - BH));
            gripRect = new Rectangle(px, py, GW, BH);
            toolRect = new Rectangle(px + GW + GAP, py, BW, BH);
            font = new Font("SansSerif", Font.BOLD, 14);
            small = new Font("SansSerif", Font.PLAIN, 10);
            setBounds(new Rectangle(px, py, w, BH));
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx) {
            if (!visible) return;
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gc.setColor(new Color(60, 60, 60, 220));
            gc.fillRoundRect(gripRect.x, gripRect.y, gripRect.width, gripRect.height, 5, 5);
            gc.setColor(new Color(200, 200, 200));
            for (int i = 0; i < 3; i++) {
                gc.fillOval(gripRect.x + 3, gripRect.y + 9 + i * 7, 3, 3);
                gc.fillOval(gripRect.x + 8, gripRect.y + 9 + i * 7, 3, 3);
            }
            boolean on = armed != null;
            Color yellow = getSettings().getColor(ENTRY_COLOR, DEF_ENTRY_COLOR);
            gc.setColor(on ? yellow.brighter() : yellow);
            gc.fillRoundRect(toolRect.x, toolRect.y, toolRect.width, toolRect.height, 6, 6);
            if (on) {
                gc.setColor(Color.WHITE);
                gc.setStroke(new BasicStroke(2f));
                gc.drawRoundRect(toolRect.x, toolRect.y, toolRect.width, toolRect.height, 6, 6);
            }
            var frc = new java.awt.font.FontRenderContext(null, true, true);
            gc.setColor(new Color(17, 17, 17));
            centred(gc, frc, font, get("BTN_TOOL"), toolRect.getCenterX(), toolRect.y + 18);
            centred(gc, frc, small, get("BTN_TOOL_CAPTION"), toolRect.getCenterX(), toolRect.y + 32);
        }

        /** Glyph outlines centred on their visible bounds (the platform lays text out differently from AWT). */
        private void centred(Graphics2D gc, java.awt.font.FontRenderContext frc, Font f, String text, double cx, double baseline) {
            var gv = f.createGlyphVector(frc, text);
            java.awt.geom.Rectangle2D v = gv.getVisualBounds();
            gc.fill(gv.getOutline((float) (cx - v.getWidth() / 2.0 - v.getX()), (float) baseline));
        }

        @Override
        public boolean contains(double x, double y, DrawContext ctx) {
            return (gripRect != null && gripRect.contains(x, y)) || (toolRect != null && toolRect.contains(x, y));
        }

        @Override
        public boolean isVisible(DrawContext ctx) {
            return getSettings().getBoolean(SHOW_PANEL, true);
        }
    }

    /** Drag handle for the panel. The platform reliably drags only ABSOLUTE (time/price)
     *  ResizePoints - a relative (pixel) one gets one onResize and then the chart takes the
     *  drag over. So the grip is absolute, re-anchored every layout to the panel's screen spot
     *  (translate2Time/Value), and onResize converts it back to pixels: the panel stays fixed
     *  on screen and does not scroll with the chart. */
    private class PanelGrip extends ResizePoint {
        PanelGrip() {
            super(ALL, true);
            setSnapToLocation(false);
            setFillColor(new Color(0, 0, 0, 0));
            setOutlineColor(new Color(0, 0, 0, 0));
        }

        @Override
        public void layout(DrawContext ctx) {
            if (activeRP != this && panel != null && panel.gripRect != null) {
                setLocation(ctx.translate2Time(panel.gripRect.getCenterX()), ctx.translate2Value(panel.gripRect.getCenterY()));
            }
            super.layout(ctx);
        }

        // NO contains() override: during a drag the platform keeps hit-testing the point at its
        // moving location; a custom rectangle there makes it drop the drag after one onResize.

        @Override
        public boolean isVisible(DrawContext ctx) {
            return getSettings().getBoolean(SHOW_PANEL, true);
        }
    }

    /** The font of the labels (the Font setting; Futures Tester uses a bold 12). */
    private Font labelFont() {
        FontInfo fi = getSettings().getFont(FONT);
        return fi != null ? fi.getFont() : new Font("SansSerif", Font.BOLD, 12);
    }

    /**
     * A label as on the tester's box: coloured text on a dark rounded plate, its vertical centre at {@code cy}, starting at
     * {@code x} and kept inside the chart (a box near the right edge keeps its labels left of the price scale).
     * Returns the plate.
     */
    static Rectangle plate(Graphics2D gc, Font font, String text, int x, int cy, Color color, Rectangle bounds) {
        var frc = gc.getFontRenderContext();
        var gv = font.createGlyphVector(frc, text);
        java.awt.geom.Rectangle2D v = gv.getVisualBounds();
        int w = (int) Math.ceil(v.getWidth()) + 10, h = 20;
        int left = Math.max(bounds.x + 4, Math.min(x, bounds.x + bounds.width - w - 4));
        Rectangle r = new Rectangle(left, cy - h / 2, w, h);
        gc.setColor(PLATE);
        gc.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        gc.setColor(color);
        gc.fill(gv.getOutline((float) (r.x + 5 - v.getX()), (float) (cy - v.getY() - v.getHeight() / 2.0)));
        return r;
    }

    private static Rectangle plateRect(Font font, String text, int x, int cy, Rectangle bounds) {
        var frc = new java.awt.font.FontRenderContext(null, true, true);
        java.awt.geom.Rectangle2D v = font.createGlyphVector(frc, text).getVisualBounds();
        int w = (int) Math.ceil(v.getWidth()) + 10;
        int left = Math.max(bounds.x + 4, Math.min(x, bounds.x + bounds.width - w - 4));
        return new Rectangle(left, cy - 10, w, 20);
    }

    /**
     * The box as the tester draws it (D-090): target zone green and stop zone red at 20 %, 2 px lines (entry yellow), and
     * three labels - the target's toward the entry, the entry's on the target side, the stop's beyond the stop - plus a
     * warning when one contract costs more than the risk or the limit capped the contracts. A box whose stop and target
     * are no longer on opposite sides (after a drag) shows its lines and a hint.
     */
    private class BoxFigure extends Figure {
        private final Box b;
        private boolean ok;
        private int x1, x2, yEntry, yStop, yTarget;
        private String[] texts;
        private boolean valid;
        private final ArrayList<Rectangle> plates = new ArrayList<>();
        Rectangle deleteRect;

        BoxFigure(Box b) {
            this.b = b;
        }

        @Override
        public void layout(DrawContext ctx) {
            plates.clear();
            deleteRect = null;
            texts = null;
            ok = b.valid();
            if (!ok) return;
            Settings s = getSettings();
            DataContext dc = ctx.getDataContext();
            Instrument instr = dc.getInstrument();
            Rectangle bd = ctx.getBounds();
            x1 = ctx.translateTime(b.start);
            x2 = ctx.translateTime(b.end);
            if (x2 < x1 + 2) x2 = x1 + 2;
            yEntry = ctx.translateValue(b.entry);
            yStop = ctx.translateValue(b.stop);
            yTarget = ctx.translateValue(b.target);
            RiskSize.Result r = size(dc, b.entry, b.stop, b.target);
            valid = r.valid();
            Rectangle area = new Rectangle(x1, Math.min(yStop, yTarget), x2 - x1, Math.abs(yStop - yTarget));
            if (!s.getBoolean(SHOW_LABELS, true)) {
                setBounds(area);
                return;
            }
            Font font = labelFont();
            texts = valid ? Labels.texts(r, rootSymbol(instr), s.getBoolean(SHOW_PRICES, false), instr::format, b.entry, b.stop, b.target)
                    : new String[]{null, get("HINT_TARGET_OTHER_SIDE"), null, null};
            int[] ys = labelYs();
            for (int i = 0; i < 3; i++) if (texts[i] != null) plates.add(plateRect(font, texts[i], x1 + 6, ys[i], bd));
            if (texts[3] != null) plates.add(plateRect(font, texts[3], x2 + 6, yEntry, bd));
            // the delete button glued to the right of the entry label
            Rectangle entryPlate = plateRect(font, texts[1], x1 + 6, ys[1], bd);
            deleteRect = new Rectangle(entryPlate.x + entryPlate.width + 3, entryPlate.y + 2, 16, 16);
            Rectangle all = area;
            for (Rectangle pr : plates) all = all.union(pr);
            setBounds(all.union(deleteRect));
        }

        /** Vertical centres of the target, entry and stop labels (the tester's 14 px offsets). */
        private int[] labelYs() {
            return new int[]{
                    yTarget + (yTarget < yEntry ? 14 : -14),
                    yEntry + (yStop > yEntry ? -14 : 14),
                    yStop + (yStop > yEntry ? 14 : -14)};
        }

        boolean bodyContains(double x, double y) {
            if (!ok || onPanel(x, y)) return false;
            if (x >= x1 && x <= x2 && y >= Math.min(yStop, yTarget) && y <= Math.max(yStop, yTarget)) return true;
            for (Rectangle pr : plates) if (pr.contains(x, y)) return true;
            return false;
        }

        @Override
        public boolean contains(double x, double y, DrawContext ctx) {
            return bodyContains(x, y) || (deleteRect != null && deleteRect.contains(x, y));
        }

        @Override
        public boolean isVisible(DrawContext ctx) {
            return true;
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx) {
            studySelected = ctx.isSelected();
            if (!ok) return;
            Settings s = getSettings();
            int w = x2 - x1;
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color up = s.getColor(TARGET_COLOR, DEF_TARGET_COLOR), down = s.getColor(STOP_COLOR, DEF_STOP_COLOR),
                    yellow = s.getColor(ENTRY_COLOR, DEF_ENTRY_COLOR);
            if (valid) {
                gc.setColor(s.getColor(TARGET_FILL, DEF_TARGET_FILL));
                gc.fillRect(x1, Math.min(yEntry, yTarget), w, Math.abs(yTarget - yEntry));
                gc.setColor(s.getColor(STOP_FILL, DEF_STOP_FILL));
                gc.fillRect(x1, Math.min(yEntry, yStop), w, Math.abs(yStop - yEntry));
            }
            gc.setStroke(new BasicStroke(2f));
            gc.setColor(up);
            gc.drawLine(x1, yTarget, x2, yTarget);
            gc.setColor(down);
            gc.drawLine(x1, yStop, x2, yStop);
            gc.setColor(yellow);
            gc.drawLine(x1, yEntry, x2, yEntry);
            if (texts != null) {
                Font font = labelFont();
                Rectangle bd = ctx.getBounds();
                int[] ys = labelYs();
                Color[] colors = {up, yellow, down};
                for (int i = 0; i < 3; i++) if (texts[i] != null) plate(gc, font, texts[i], x1 + 6, ys[i], valid ? colors[i] : WARN, bd);
                if (texts[3] != null) plate(gc, font, texts[3], x2 + 6, yEntry, WARN, bd);
            }
            drawDelete(gc);
        }

        private void drawDelete(Graphics2D gc) {
            if (deleteRect == null) return;
            Rectangle d = deleteRect;
            gc.setColor(new Color(10, 10, 14, 217));
            gc.fillRoundRect(d.x, d.y, d.width, d.height, 5, 5);
            gc.setColor(new Color(0xe2, 0xe2, 0xe8));
            gc.setStroke(new BasicStroke(1.6f));
            int m = 4;
            gc.drawLine(d.x + m, d.y + m, d.x + d.width - m, d.y + d.height - m);
            gc.drawLine(d.x + d.width - m, d.y + m, d.x + m, d.y + d.height - m);
        }
    }

    /** Scales a colour's own alpha by the label opacity setting (percent). */
    private static Color withOpacity(Color c, int percent) {
        int a = Math.round(c.getAlpha() * Math.max(0, Math.min(100, percent)) / 100f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static FontMetrics metricsFor(Font font) {
        var img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.dispose();
        return fm;
    }
}
